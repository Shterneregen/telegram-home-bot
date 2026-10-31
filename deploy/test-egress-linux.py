#!/usr/bin/env python3
"""Run ONLY via unshare --net: exercises real nftables against local test sockets."""
import importlib.util
import os
from pathlib import Path
import socket
import subprocess
import sys

if os.readlink("/proc/self/ns/net") == os.readlink("/proc/1/ns/net"):
    raise SystemExit("Refusing to alter host network: invoke with unshare --net")
spec = importlib.util.spec_from_file_location("guard", Path(__file__).with_name("telegram-entrypoint.py"))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)
subprocess.run(["ip", "link", "set", "lo", "up"], check=True)
servers = []
for family, address in [(socket.AF_INET, "127.0.0.1"), (socket.AF_INET, "127.0.0.1"), (socket.AF_INET6, "::1")]:
    server = socket.socket(family)
    server.bind((address, 0))
    server.listen(10)
    servers.append(server)
port = servers[0].getsockname()[1]

def probe(server, allowed):
    family = int(server.family)
    address, port = server.getsockname()[:2]
    code = "import socket; s=socket.socket(%d); s.settimeout(2); s.connect((%r,%d))" % (family, address, port)
    bootstrap = "import importlib.util; spec=importlib.util.spec_from_file_location('guard',%r); g=importlib.util.module_from_spec(spec); spec.loader.exec_module(g); g.drop_privileges(); " % str(Path(__file__).with_name("telegram-entrypoint.py"))
    verify = "from pathlib import Path; status=dict(line.split(':',1) for line in Path('/proc/self/status').read_text().splitlines()); assert status['NoNewPrivs'].strip()=='1'; assert all(int(status[k].strip(),16)==8192 for k in ['CapEff','CapPrm','CapBnd','CapInh','CapAmb']); "
    verify += "assert status['Seccomp'].strip()=='2'; "
    execute = "import os; os.execv(%r,[%r,'-c',%r])" % (sys.executable, sys.executable, verify + code)
    result = subprocess.run([sys.executable, "-c", bootstrap + execute], capture_output=True)
    assert (result.returncode == 0) == allowed, result.stderr.decode()

for replacement in [False, True]:
    rules = guard.policy("127.0.0.1", port, [], [], [], replacement)
    subprocess.run(["nft", "-f", "-"], input=rules, text=True, check=True)
    probe(servers[0], True)
    probe(servers[1], False)
    probe(servers[2], False)
# The allowed endpoint going away must not permit a different destination.
servers[0].close()
probe(servers[1], False)
probe(servers[2], False)
# Exercise the inherited filter with NET_RAW present: ordinary unprivileged tests
# cannot distinguish its denial from the kernel's capability check.
socket_checks = '''
import ctypes
import errno
import socket
for family, kind, protocol in [(17, socket.SOCK_RAW, 0), (17, socket.SOCK_DGRAM, 0),
                               (44, socket.SOCK_RAW, 0), (2, 10, 0),
                               (2, socket.SOCK_RAW, socket.IPPROTO_TCP),
                               (10, socket.SOCK_RAW, socket.IPPROTO_UDP)]:
    try:
        socket.socket(family, kind, protocol).close()
    except OSError as error:
        assert error.errno == errno.EPERM, error
    else:
        raise AssertionError('forbidden raw/packet socket opened')
libc = ctypes.CDLL(None, use_errno=True)
number = ctypes.CDLL('libseccomp.so.2').seccomp_syscall_resolve_name(b'socket')
for family in (17, 44):
    descriptor = libc.syscall(number, ctypes.c_ulong((1 << 32) | family), socket.SOCK_RAW, 0)
    assert descriptor == -1 and ctypes.get_errno() == errno.EPERM
for family, protocol in [(socket.AF_INET, socket.IPPROTO_ICMP), (socket.AF_INET6, socket.IPPROTO_ICMPV6)]:
    with socket.socket(family, socket.SOCK_RAW, protocol) as raw:
        level, option = (0, 3) if family == socket.AF_INET else (41, 36)
        try:
            raw.setsockopt(level, option, 1)
        except OSError as error:
            assert error.errno == errno.EPERM, error
        else:
            raise AssertionError('raw IP header injection enabled')
'''
bootstrap = (
    "import importlib.util, os, sys; "
    "spec=importlib.util.spec_from_file_location('guard',%r); "
    "g=importlib.util.module_from_spec(spec); spec.loader.exec_module(g); "
    "g.drop_privileges(); os.execv(sys.executable,[sys.executable,'-c',%r])"
) % (str(Path(__file__).with_name("telegram-entrypoint.py").resolve()), socket_checks)
subprocess.run([sys.executable, "-c", bootstrap], check=True)
subprocess.run(["nft", "list", "table", "inet", guard.TABLE], check=True)
print("PASS: proxy, IPv4/IPv6 rejection, atomic replacement, outage; packet/raw bypass denied; ICMP allowed")
