"""Offline contract tests using the actual Compose parser, no Docker Engine needed."""
import importlib.util
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('validation', Path(__file__).with_name('validate-telegram-deployment.py'))
validation = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validation)


class ComposeTransportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        docker = shutil.which('docker')
        if not docker or subprocess.run([docker, 'compose', 'version'], capture_output=True).returncode:
            raise unittest.SkipTest('Docker Compose CLI is required (engine is not used)')
        cls.compose = [docker, 'compose']

    def test_real_env_syntax_cannot_disguise_mtproto_as_bot_api(self):
        variants = [
            'TELEGRAM_TRANSPORT=mtproto', 'TELEGRAM_TRANSPORT = mtproto',
            'TELEGRAM_TRANSPORT: mtproto', ' export TELEGRAM_TRANSPORT=mtproto',
            'TELEGRAM_TRANSPORT="mtproto"', "TELEGRAM_TRANSPORT='mtproto'",
            'TELEGRAM_TRANSPORT=mtproto # comment',
            'MODE=mtproto\nTELEGRAM_TRANSPORT=${MODE}',
            'TELEGRAM_TRANSPORT=bot-api\nTELEGRAM_TRANSPORT=mtproto',
        ]
        for content in variants:
            with self.subTest(content=content), tempfile.TemporaryDirectory() as name:
                env = Path(name) / 'thb.env'
                env.write_text(content + '\n')
                validation.validate('mtproto', str(env), self.compose)
                with self.assertRaises(ValueError):
                    validation.validate('bot-api', str(env), self.compose)

    def test_missing_transport_defaults_only_to_bot_api(self):
        with tempfile.TemporaryDirectory() as name:
            env = Path(name) / 'thb.env'
            env.write_text('TELEGRAM_ENABLED=false\n')
            validation.validate('bot-api', str(env), self.compose)
            with self.assertRaises(ValueError):
                validation.validate('mtproto', str(env), self.compose)

    def test_effective_compose_override_cannot_disguise_transport(self):
        with tempfile.TemporaryDirectory() as name:
            env = Path(name) / 'thb.env'
            env.write_text('TELEGRAM_TRANSPORT=bot-api\n')
            compose = Path(name) / 'compose.yml'
            compose.write_text('services:\n  thb:\n    image: unused\n    environment:\n      TELEGRAM_TRANSPORT: mtproto\n')
            with self.assertRaises(ValueError):
                validation.validate('bot-api', str(env), self.compose, ['-f', str(compose)])


if __name__ == '__main__':
    unittest.main()
