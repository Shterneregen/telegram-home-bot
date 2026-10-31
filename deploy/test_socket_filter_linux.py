"""Unprivileged seccomp tests: only a child process is restricted, no packets sent."""
from pathlib import Path
import subprocess
import sys
import unittest


@unittest.skipUnless(sys.platform == "linux", "requires Linux seccomp and libseccomp2")
class SocketFilterTest(unittest.TestCase):
    def test_filter_is_inherited_across_exec_and_preserves_normal_sockets(self):
        entrypoint = str(Path(__file__).with_name("telegram-entrypoint.py").resolve())
        checks = r'''
import ctypes
import errno
from pathlib import Path
import socket
status = dict(line.split(':', 1) for line in Path('/proc/self/status').read_text().splitlines())
assert status['Seccomp'].strip() == '2'
assert status['NoNewPrivs'].strip() == '1'
for family, kind, protocol in [
    (17, socket.SOCK_RAW, 0), (17, socket.SOCK_DGRAM | socket.SOCK_CLOEXEC, 0),
    (44, socket.SOCK_RAW, 0), (2, 10, 0), (0, 10 | socket.SOCK_NONBLOCK, 0),
    (2, socket.SOCK_RAW | socket.SOCK_CLOEXEC, socket.IPPROTO_TCP),
    (10, socket.SOCK_RAW, socket.IPPROTO_UDP),
]:
    try:
        socket.socket(family, kind, protocol).close()
    except OSError as error:
        assert error.errno == errno.EPERM, (family, kind, error)
    else:
        raise AssertionError(('forbidden socket was opened', family, kind))
for family in (socket.AF_INET, socket.AF_INET6):
    for kind in (socket.SOCK_STREAM, socket.SOCK_DGRAM):
        with socket.socket(family, kind):
            pass
# io_uring_setup is syscall 425 on supported AMD64/ARM64 Linux.
libc = ctypes.CDLL(None, use_errno=True)
assert libc.syscall(425, 0, 0) == -1 and ctypes.get_errno() == errno.EPERM
seccomp = ctypes.CDLL('libseccomp.so.2')
number = seccomp.seccomp_syscall_resolve_name(b'socket')
for family in (17, 44):
    # A libc socket wrapper would truncate these arguments before the syscall.
    # Invalid type 0 would normally return EINVAL even without NET_RAW.
    result = libc.syscall(number, ctypes.c_ulong((1 << 32) | family), 0, 0)
    assert result == -1 and ctypes.get_errno() == errno.EPERM
number = seccomp.seccomp_syscall_resolve_name(b'setsockopt')
for level, option in ((0, 3), (41, 36)):
    for high in (0, 1 << 32):
        # Without the filter, fd -1 returns EBADF. No NET_RAW needed to prove denial.
        result = libc.syscall(number, -1, ctypes.c_ulong(high | level),
                              ctypes.c_ulong(high | option), 0, 0)
        assert result == -1 and ctypes.get_errno() == errno.EPERM
with socket.socket() as normal:
    normal.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
'''
        bootstrap = (
            "import importlib.util, os, sys; "
            f"spec=importlib.util.spec_from_file_location('guard', {entrypoint!r}); "
            "guard=importlib.util.module_from_spec(spec); spec.loader.exec_module(guard); "
            "guard.install_socket_filter(); "
            f"os.execv(sys.executable, [sys.executable, '-c', {checks!r}])"
        )
        result = subprocess.run([sys.executable, "-c", bootstrap], capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)


if __name__ == "__main__":
    unittest.main()
