#!/usr/bin/env python3
"""Install the MTProto process egress policy before starting Java. Never log credentials."""
import ctypes
import errno
import ipaddress
import json
import os
from pathlib import Path
import socket
import subprocess
import sys

UID = 10001
TABLE = "thb_mtproto"
SOCKET_POLICY_VERSION = 3


def install_socket_filter():
    """Inherited by JVM/children. Keep IP ICMP for fping, deny L2 bypasses."""
    seccomp = ctypes.CDLL("libseccomp.so.2", use_errno=True)
    class Comparison(ctypes.Structure):
        _fields_ = [("arg", ctypes.c_uint), ("op", ctypes.c_int),
                    ("a", ctypes.c_uint64), ("b", ctypes.c_uint64)]
    seccomp.seccomp_init.argtypes = [ctypes.c_uint32]
    seccomp.seccomp_init.restype = ctypes.c_void_p
    seccomp.seccomp_syscall_resolve_name.argtypes = [ctypes.c_char_p]
    seccomp.seccomp_syscall_resolve_name.restype = ctypes.c_int
    seccomp.seccomp_rule_add_array.argtypes = [
        ctypes.c_void_p, ctypes.c_uint32, ctypes.c_int, ctypes.c_uint,
        ctypes.POINTER(Comparison)]
    seccomp.seccomp_load.argtypes = [ctypes.c_void_p]
    seccomp.seccomp_release.argtypes = [ctypes.c_void_p]
    context = seccomp.seccomp_init(0x7fff0000)  # SCMP_ACT_ALLOW
    if not context:
        raise RuntimeError("Cannot allocate socket filter")
    try:
        def deny(name, comparisons=()):
            syscall = seccomp.seccomp_syscall_resolve_name(name.encode())
            if syscall == -1:
                raise RuntimeError("Missing syscall definition: " + name)
            args = (Comparison * len(comparisons))(*[Comparison(*c) for c in comparisons])
            result = seccomp.seccomp_rule_add_array(
                context, 0x00050000 | errno.EPERM, syscall, len(args), args)
            if result != 0:
                raise RuntimeError("Cannot configure socket filter")
        # AF_PACKET, AF_XDP, and the legacy AF_INET/SOCK_PACKET API.
        # socket arguments are C ints: mask the low 32 bits, as the kernel does.
        # A full 64-bit comparison would let direct syscall callers add high bits.
        for family in (17, 44):
            deny("socket", [(0, 7, 0xffffffff, family)])
        deny("socket", [(1, 7, 0xf, 10)])  # SCMP_CMP_MASKED_EQ, strip socket flags
        # Restrict IP raw sockets to ICMP; arbitrary raw TCP is unnecessary.
        for family, protocol in ((2, 1), (10, 58)):
            deny("socket", [(0, 7, 0xffffffff, family), (1, 7, 0xf, 3), (2, 1, protocol, 0)])
        # An ICMP raw socket must not supply an arbitrary IP protocol/header.
        # setsockopt takes C ints too; check low bits even for direct syscalls.
        for level, option in ((0, 3), (41, 36)):  # IP_HDRINCL, IPV6_HDRINCL
            deny("setsockopt", [(1, 7, 0xffffffff, level), (2, 7, 0xffffffff, option)])
        # io_uring socket operations don't traverse the socket syscall filter.
        deny("io_uring_setup")
        if seccomp.seccomp_load(context) != 0:
            raise RuntimeError("Cannot install socket filter")
    finally:
        seccomp.seccomp_release(context)

def resolve(host):
    return sorted({str(ipaddress.ip_address(row[4][0])) for row in socket.getaddrinfo(host, None, type=socket.SOCK_STREAM)})

def destination(address):
    ip = ipaddress.ip_address(address)
    return ("ip" if ip.version == 4 else "ip6") + " daddr " + str(ip)

def policy(proxy, port, dns, lan, weather, exists):
    if not 1 <= port <= 65535:
        raise ValueError("Invalid proxy port")
    lines = [f"delete table inet {TABLE}"] if exists else []
    lines += [f"table inet {TABLE} {{", "chain output { type filter hook output priority -20; policy accept;",
              f"meta skuid {UID} jump restricted", "}", "chain restricted {",
              "ct direction reply ct state established,related accept",
              f"{destination(proxy)} tcp dport {port} counter accept"]
    for address in dns:
        lines += [f"{destination(address)} udp dport 53 accept", f"{destination(address)} tcp dport 53 accept"]
    for cidr in lan:
        net = ipaddress.ip_network(cidr, strict=False)
        if net.version != 4 or not any(net.subnet_of(ipaddress.ip_network(x)) for x in ("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")):
            raise ValueError("LAN allowlist must contain only RFC1918 IPv4 networks")
        lines += [f"ip daddr {net} accept"]
    for address in weather:
        lines += [f"{destination(address)} tcp dport 443 accept"]
    lines += ["counter reject", "}", "}"]
    return "\n".join(lines) + "\n"

def drop_privileges():
    """Drop to reserved UID; retain only NET_RAW for fping, including across exec."""
    install_socket_filter()
    libc = ctypes.CDLL(None, use_errno=True)
    def prctl(option, arg=0, arg2=0):
        if libc.prctl(ctypes.c_int(option), ctypes.c_ulong(arg), ctypes.c_ulong(arg2), ctypes.c_ulong(0), ctypes.c_ulong(0)) != 0:
            raise OSError(ctypes.get_errno(), "Cannot restrict process capabilities")
    last = int(Path("/proc/sys/kernel/cap_last_cap").read_text())
    if last > 63:
        raise ValueError("Unsupported capability ABI")
    prctl(8, 1)  # PR_SET_KEEPCAPS until capset after setuid
    for capability in range(last + 1):
        if capability != 13:
            prctl(24, capability)  # PR_CAPBSET_DROP
    os.setgroups([])
    os.setgid(UID)
    os.setuid(UID)
    class Header(ctypes.Structure):
        _fields_ = [("version", ctypes.c_uint32), ("pid", ctypes.c_int)]
    class Data(ctypes.Structure):
        _fields_ = [("effective", ctypes.c_uint32), ("permitted", ctypes.c_uint32), ("inheritable", ctypes.c_uint32)]
    header = Header(0x20080522, 0)  # Linux capability ABI v3
    data = (Data * 2)(Data(1 << 13, 1 << 13, 1 << 13), Data(0, 0, 0))
    if libc.capset(ctypes.byref(header), ctypes.byref(data)) != 0:
        raise OSError(ctypes.get_errno(), "Cannot drop capabilities")
    prctl(8, 0)
    prctl(38, 1)  # PR_SET_NO_NEW_PRIVS
    prctl(47, 2, 13)  # PR_CAP_AMBIENT_RAISE NET_RAW

def main():
    env = os.environ
    if env.get("TELEGRAM_ENABLED", "false").lower() != "true" or env.get("TELEGRAM_TRANSPORT", "bot-api") != "mtproto":
        os.execvp("java", ["java", "-jar", "/app/app.jar"])
    if os.geteuid() != 0:
        raise ValueError("MTProto launcher must initially run as root")
    host = env.get("TELEGRAM_PROXY_HOST", "")
    port = int(env.get("TELEGRAM_PROXY_PORT", "443"))
    addresses = resolve(host)
    if not host or not addresses:
        raise ValueError("Missing proxy address")
    # Pin one resolved endpoint; native DNS changes cannot escape this rule.
    proxy = addresses[0]
    dns = []
    for line in Path("/etc/resolv.conf").read_text().splitlines():
        words = line.split()
        if len(words) >= 2 and words[0] == "nameserver":
            dns.append(str(ipaddress.ip_address(words[1])))
    lan = [x.strip() for x in env.get("TELEGRAM_EGRESS_LAN_CIDRS", "").split(",") if x.strip()]
    weather = resolve("api.openweathermap.org") if env.get("OPENWEATHER_ENABLED", "false").lower() == "true" else []
    exists = subprocess.run(["nft", "list", "table", "inet", TABLE], capture_output=True).returncode == 0
    rules = policy(proxy, port, dns, lan, weather, exists)
    # A single nft transaction replaces only our table, without an unprotected interval.
    subprocess.run(["nft", "-f", "-"], input=rules, text=True, capture_output=True, check=True)
    subprocess.run(["nft", "list", "table", "inet", TABLE], capture_output=True, check=True)
    data = Path(env.get("TELEGRAM_TDLIB_DATA_DIR", "/app/data/tdlib")).resolve()
    if not data.is_relative_to(Path("/app/data")):
        raise ValueError("TDLib directory must be inside /app/data")
    env["TELEGRAM_TDLIB_DATA_DIR"] = str(data)
    directory = Path("/run/thb")
    directory.mkdir(mode=0o755, exist_ok=True)
    os.chown(directory, 0, 0)
    os.chmod(directory, 0o755)
    record = directory / "egress.json"
    record.write_text(json.dumps({"pid": os.getpid(), "uid": UID, "host": host, "port": port,
        "address": proxy, "socketPolicy": SOCKET_POLICY_VERSION,
        "boot": Path("/proc/sys/kernel/random/boot_id").read_text().strip()}))
    os.chown(record, 0, 0)
    os.chmod(record, 0o444)
    drop_privileges()
    os.execvp("java", ["java", "-jar", "/app/app.jar"])


if __name__ == "__main__":
    try:
        main()
    except Exception:
        print("MTProto guarded startup failed. Check NET_ADMIN, proxy/DNS, port and data permissions. Java was not started.", file=sys.stderr)
        sys.exit(1)
