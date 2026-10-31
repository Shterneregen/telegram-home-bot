#!/usr/bin/env python3
"""Read env_file with Compose itself. Never print resolved configuration/secrets.

Gradle embeds this script in the SSH command; no helper installation is needed.
Arguments: expected transport, absolute env path, Compose command, --, Compose file options.
"""
import json
from pathlib import Path
import subprocess
import sys


def validate(expected, env_path, compose, files=()):
    if expected not in ("mtproto", "bot-api") or not Path(env_path).is_file():
        raise ValueError("Missing deployment environment or invalid transport")
    # A minimal Compose service reads the *unoverridden* env_file. Otherwise the
    # MTProto override could mask a mismatched file that a later rollback uses.
    probe = {"services": {"thb": {"image": "unused", "env_file": [str(Path(env_path).resolve())]}}}
    result = subprocess.run(
        compose + ["--env-file", env_path, "-f", "-", "config", "--format", "json"],
        input=json.dumps(probe), capture_output=True, text=True, timeout=30,
    )
    if result.returncode:
        raise ValueError("Compose could not validate deployment environment")
    environment = json.loads(result.stdout)["services"]["thb"]["environment"]
    actual = environment.get("TELEGRAM_TRANSPORT") or "bot-api"
    if actual != expected:
        raise ValueError("Remote TELEGRAM_TRANSPORT does not match -PmtprotoDeployment; deployment stopped")
    if files:
        effective = subprocess.run(
            compose + ["--env-file", env_path] + list(files) + ["config", "--format", "json"],
            capture_output=True, text=True, timeout=30,
        )
        if effective.returncode:
            raise ValueError("Compose could not validate deployment configuration")
        actual = json.loads(effective.stdout)["services"]["thb"]["environment"].get("TELEGRAM_TRANSPORT") or "bot-api"
        if actual != expected:
            raise ValueError("Effective Compose transport does not match deployment mode")


if __name__ == "__main__":
    try:
        separator = sys.argv.index("--", 3)
        validate(sys.argv[1], sys.argv[2], sys.argv[3:separator], sys.argv[separator + 1:])
    except Exception:
        # Compose diagnostics, parsed values and exceptions can contain secrets.
        print("Deployment transport validation failed; check Compose, Python 3, environment and -PmtprotoDeployment", file=sys.stderr)
        sys.exit(1)
