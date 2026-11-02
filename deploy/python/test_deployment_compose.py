"""Offline contract tests using the actual Compose parser, no Docker Engine needed."""
import importlib.util
import json
import os
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

    def test_relocated_compose_preserves_local_env_and_mounts(self):
        root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as name:
            directory = Path(name)
            # A project root .env and a relative THB_ENV_FILE must both resolve
            # against --project-directory, even when files live under deploy/.
            for relative_env in (None, '.env', 'custom.env'):
                (directory / (relative_env or '.env')).write_text('TELEGRAM_TRANSPORT=bot-api\n')
                for mtproto in (False, True):
                    with self.subTest(env=relative_env, mtproto=mtproto):
                        environment = dict(os.environ,
                                           THB_APP_DIR='/opt/telegram-home-bot',
                                           THB_DATA_DIR='/var/lib/telegram-home-bot',
                                           THB_CONFIG_DIR='/etc/telegram-home-bot')
                        environment.pop('THB_ENV_FILE', None)
                        if relative_env:
                            environment['THB_ENV_FILE'] = relative_env
                        args = self.compose + ['--project-directory', str(directory),
                                               '--env-file', str(directory / '.env'),
                                               '-f', str(root / 'deploy/docker-compose.yml')]
                        if mtproto:
                            args += ['-f', str(root / 'deploy/docker-compose.mtproto.yml')]
                        result = subprocess.run(args + ['config', '--format', 'json'],
                                                env=environment, capture_output=True, text=True, check=True)
                        service = json.loads(result.stdout)['services']['thb']
                        self.assertEqual('mtproto' if mtproto else 'bot-api',
                                         service['environment']['TELEGRAM_TRANSPORT'])
                        self.assertEqual('host', service['network_mode'])
                        mounts = {mount['target']: mount for mount in service['volumes']}
                        self.assertEqual('/opt/telegram-home-bot/thb.jar', mounts['/app/app.jar']['source'])
                        self.assertTrue(mounts['/app/app.jar']['read_only'])
                        self.assertEqual('/var/lib/telegram-home-bot', mounts['/app/data']['source'])

    def test_remote_flat_compose_layout_with_staging_names(self):
        root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as name:
            directory = Path(name)
            env = directory / 'thb.env'
            env.write_text('TELEGRAM_TRANSPORT=bot-api\n')
            environment = dict(os.environ, THB_ENV_FILE=str(env))
            for staged in (False, True):
                suffix = '.new' if staged else ''
                files = []
                for filename in ('docker-compose.yml', 'docker-compose.mtproto.yml'):
                    destination = directory / (filename + suffix)
                    shutil.copyfile(root / 'deploy' / filename, destination)
                    files += ['-f', str(destination)]
                for mtproto in (False, True):
                    with self.subTest(staged=staged, mtproto=mtproto):
                        args = self.compose + ['--env-file', str(env)] + (files if mtproto else files[:2])
                        result = subprocess.run(args + ['config', '--format', 'json'], cwd=directory,
                                                env=environment, capture_output=True, text=True, check=True)
                        service = json.loads(result.stdout)['services']['thb']
                        self.assertEqual('mtproto' if mtproto else 'bot-api',
                                         service['environment']['TELEGRAM_TRANSPORT'])

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
