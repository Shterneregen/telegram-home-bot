"""Execute generated deployment shells in temporary directories with a fake Docker.

Generate build/deployment-policy-{true,false}.json with the offline Gradle init test first.
No SSH, real Docker, credentials, network configuration or application processes are used.
"""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parent.parent


@unittest.skipUnless(sys.platform == "linux", "requires Linux shell and coreutils")
class DeploymentCommandsTest(unittest.TestCase):
    def scripts(self, mtproto, directory):
        fixture = ROOT / "build" / f"deployment-policy-{str(mtproto).lower()}.json"
        if not fixture.exists():
            self.skipTest("run testDeploymentPolicy for both transports first")
        scripts = json.loads(fixture.read_text())
        replacements = {
            "/opt/telegram-home-bot": directory / "app",
            "/etc/telegram-home-bot": directory / "config",
            "/var/lib/telegram-home-bot": directory / "data",
            "/var/backups/telegram-home-bot": directory / "backup",
            "/var/cache/telegram-home-bot": directory / "cache",
        }
        for original, temporary in replacements.items():
            temporary.mkdir(exist_ok=True)
            scripts = {name: script.replace(original, str(temporary)) for name, script in scripts.items()}
        for script in scripts.values():
            subprocess.run(["sh", "-n"], input=script, text=True, check=True, timeout=5)
        return scripts

    def environment(self, directory):
        binary = directory / "bin"
        binary.mkdir()
        docker = binary / "docker"
        docker.write_text(
            '#!/bin/sh\n'
            'echo "$*" >> "$TEST_DOCKER_LOG"\n'
            'case "$*" in *"-f - "*) cat >/dev/null;; esac\n'
            'case "$*" in *"--format json"*) printf "%s" "$TEST_COMPOSE_JSON"; exit 0;; esac\n'
            'case "$*" in *" up "*) exit 1;; esac\n'
            'exit 0\n'
        )
        docker.chmod(0o700)
        return dict(os.environ, PATH=str(binary) + ":" + os.environ["PATH"],
                    TEST_DOCKER_LOG=str(directory / "docker.log"))

    def run_script(self, script, environment):
        return subprocess.run(["sh", "-c", script], env=environment,
                              capture_output=True, text=True, timeout=5)

    def test_remote_transport_validation_precedes_compose(self):
        for mtproto in (True, False):
            for transport in ("mtproto", "bot-api", "'mtproto'", '"bot-api"', "", "unknown"):
                with self.subTest(mtproto=mtproto, transport=transport), tempfile.TemporaryDirectory() as name:
                    directory = Path(name)
                    script = self.scripts(mtproto, directory)['deployValidateRemote']
                    environment = self.environment(directory)
                    (directory / "config/thb.env").write_text(
                        f"TELEGRAM_TRANSPORT={transport}\nDB_URL=jdbc:h2:/app/data/test\nSSL_ENABLED=false\n")
                    actual = transport.strip("'\"") or "bot-api"
                    environment['TEST_COMPOSE_JSON'] = json.dumps({'services': {'thb': {'environment': {'TELEGRAM_TRANSPORT': actual}}}})
                    result = self.run_script(script, environment)
                    valid = actual == ("mtproto" if mtproto else "bot-api")
                    self.assertEqual(valid, result.returncode == 0, result.stderr)
                    if not valid:
                        self.assertNotIn('config --quiet', (directory / 'docker.log').read_text())

    def test_failure_only_restores_jar_for_bot_api(self):
        for mtproto in (True, False):
            for task in ('deployUp', 'deployRestart'):
                with self.subTest(mtproto=mtproto, task=task), tempfile.TemporaryDirectory() as name:
                    directory = Path(name)
                    script = self.scripts(mtproto, directory)[task]
                    environment = self.environment(directory)
                    environment['TEST_COMPOSE_JSON'] = json.dumps({'services': {'thb': {'environment': {
                        'TELEGRAM_TRANSPORT': 'mtproto' if mtproto else 'bot-api'}}}})
                    (directory / "config/thb.env").write_text(
                        "TELEGRAM_TRANSPORT=" + ("mtproto" if mtproto else "bot-api") + "\n")
                    jar = directory / "app/thb.jar"
                    jar.write_text("new")
                    (directory / "backup/thb.jar.bak").write_text("previous")
                    (directory / "cache/thb-image.tar").write_text("dummy image")
                    result = self.run_script(script, environment)
                    self.assertNotEqual(0, result.returncode)
                    self.assertEqual("new" if mtproto else "previous", jar.read_text())
                    if mtproto:
                        self.assertIn("JAR-only rollback is disabled", result.stderr)

    def test_bot_api_rollback_refuses_remote_mtproto_before_copy(self):
        with tempfile.TemporaryDirectory() as name:
            directory = Path(name)
            script = self.scripts(False, directory)['deployRollback']
            environment = self.environment(directory)
            environment['TEST_COMPOSE_JSON'] = json.dumps({'services': {'thb': {'environment': {'TELEGRAM_TRANSPORT': 'mtproto'}}}})
            (directory / "config/thb.env").write_text("TELEGRAM_TRANSPORT = mtproto\n")
            jar = directory / "app/thb.jar"
            jar.write_text("current")
            (directory / "backup/thb.jar.bak").write_text("previous")
            self.assertNotEqual(0, self.run_script(script, environment).returncode)
            self.assertEqual("current", jar.read_text())
            self.assertNotIn(' up ', (directory / "docker.log").read_text())

    def test_candidate_mismatch_preserves_active_artifacts(self):
        with tempfile.TemporaryDirectory() as name:
            directory = Path(name)
            script = self.scripts(False, directory)['deployValidateCandidate']
            environment = self.environment(directory)
            environment['TEST_COMPOSE_JSON'] = json.dumps({'services': {'thb': {'environment': {'TELEGRAM_TRANSPORT': 'mtproto'}}}})
            files = ['config/thb.env', 'app/thb.jar', 'app/docker-compose.yml']
            for path in files:
                (directory / path).write_text('current')
            (directory / 'app/docker-compose.yml.new').write_text('candidate')
            self.assertNotEqual(0, self.run_script(script, environment).returncode)
            self.assertTrue(all((directory / path).read_text() == 'current' for path in files))

    def test_rollback_refuses_missing_or_invalid_compose_output(self):
        for output in ('', '{invalid', '{}'):
            with tempfile.TemporaryDirectory() as name:
                directory = Path(name)
                script = self.scripts(False, directory)['deployRollback']
                environment = self.environment(directory)
                environment['TEST_COMPOSE_JSON'] = output
                (directory / 'config/thb.env').write_text('TELEGRAM_TRANSPORT=bot-api\n')
                (directory / 'app/thb.jar').write_text('current')
                (directory / 'backup/thb.jar.bak').write_text('previous')
                self.assertNotEqual(0, self.run_script(script, environment).returncode)
                self.assertEqual('current', (directory / 'app/thb.jar').read_text())


if __name__ == "__main__":
    unittest.main()
