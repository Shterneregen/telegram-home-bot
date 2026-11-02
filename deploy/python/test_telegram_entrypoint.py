import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("entrypoint", Path(__file__).with_name("telegram-entrypoint.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class PolicyTest(unittest.TestCase):
    def test_ipv4_ipv6_and_scoped_deny(self):
        rules = module.policy("2001:db8::1", 443, ["127.0.0.53"], ["192.168.1.0/24"], [], False)
        self.assertIn("table inet thb_mtproto", rules)
        self.assertIn("meta skuid 10001 jump restricted", rules)
        self.assertIn("ip6 daddr 2001:db8::1 tcp dport 443", rules)
        self.assertIn("counter reject", rules)
        self.assertNotIn("ct state established accept", rules)
    def test_no_public_or_injected_lan(self):
        for network in ["0.0.0.0/0", "8.8.8.0/24", "::/0", "10.0.0.0/8; accept"]:
            with self.assertRaises(ValueError):
                module.policy("192.0.2.1", 443, [], [network], [], False)
    def test_atomic_table_replacement(self):
        rules = module.policy("192.0.2.1", 443, [], [], [], True)
        self.assertTrue(rules.startswith("delete table inet thb_mtproto\n"))
        self.assertNotIn("flush ruleset", rules)
    def test_invalid_endpoint(self):
        for port in [0, 65536]:
            with self.assertRaises(ValueError):
                module.policy("192.0.2.1", port, [], [], [], False)
        with self.assertRaises(ValueError):
            module.policy("1.2.3.4; accept", 443, [], [], [], False)

if __name__ == "__main__":
    unittest.main()
