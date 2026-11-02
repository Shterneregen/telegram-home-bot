---
name: docker-compose
description: "Change THB Compose services, environment wiring, persistent mounts, health checks or MTProto overrides. Image and launcher implementation belong to docker."
---

# Docker Compose

## Sources to read for the change

- Start with the [base Compose file](../../../deploy/docker-compose.yml) and, for MTProto, its [override](../../../deploy/docker-compose.mtproto.yml).
- For invocation, local versus remote paths and deployment tasks, use the [deployment guide](../../../deploy/docs/raspberry-pi-deployment.md).
- For MTProto privileges, session storage or shutdown changes, read [guarded startup](../../../docs/telegram-mtproto.md#guarded-linux-startup) and [storage and runtime](../../../docs/telegram-mtproto.md#storage-and-runtime).

## Service contracts

- The service uses a runtime image with a read-only JAR mount at `/app/app.jar`, persistent data at `/app/data`, and read-only secrets at `/app/secrets`. Keep H2's `DB_URL` and TDLib storage inside the persistent data mount.
- Preserve the current host networking unless the task calls for a network redesign. Bridge networking is not an equivalent view of the host LAN or neighbor table; assess scanning, WoL and egress rules together.
- Read environment defaults from [.env.example](../../../.env.example) and [application.yaml](../../../src/main/resources/application.yaml). `env_file` injects variables; it does not mount a configuration file.
- Keep the health check aligned with server port, optional TLS and public `/actuator/health`.
- The MTProto override grants launcher privileges. Keep its shutdown grace period aligned with native cleanup and the application's terminal-failure watchdog.
- Do not add optional services such as SonarQube as part of unrelated deployment changes.

## Verification

Use [Compose validation](../../../docs/verification.md#compose-and-arm64-packaging) for both base and overridden configurations.
Use its `.env.example` placeholders and `config --quiet` to avoid printing resolved secrets.
For changes affecting generated deployment commands, run the relevant [deployment policy tests](../../../docs/verification.md#deployment-policy-and-launcher-tests).
