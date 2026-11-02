---
name: docker
description: "Change the THB runtime image or container launcher, including Linux ARM64 dependencies and guarded MTProto startup. Use docker-compose for service wiring and mounts."
---

# Docker runtime

## Sources to read for the change

- Inspect [Dockerfile](../../../deploy/Dockerfile) and [launcher](../../../deploy/python/telegram-entrypoint.py) before changing the image or entrypoint.
- For Pi packaging and deployment, read the relevant sections of the [deployment guide](../../../deploy/docs/raspberry-pi-deployment.md). Use the [Docker installation guide](../../../deploy/docs/raspberry-pi-3-docker-arm64.md) only for host setup.
- For launcher, privileges or egress changes, read [guarded Linux startup](../../../docs/telegram-mtproto.md#guarded-linux-startup) and [Linux Guard contracts](../../../docs/telegram-modules.md#linux-guard-and-trust-boundary).

## Runtime contracts

- The image supplies a Java 17 runtime; Compose mounts the separately built JAR at `/app/app.jar`. Building the image alone does not package the application.
- Preserve the Python entrypoint: MTProto startup installs and verifies OS policy before dropping privileges and starting Java. A direct Java entrypoint bypasses that workflow.
- Check Linux ARM64 availability and native dependencies when changing the base image or packages. Keep packages needed by network monitoring and the guarded launcher; avoid unrelated additions.
- Coordinate changes to runtime paths with Compose, launcher and `deploy/deploy.gradle`. Local deployment files live under `deploy/`; remote files remain directly under `piDir`.

## Verification

Select relevant checks from [Compose and ARM64 packaging](../../../docs/verification.md#compose-and-arm64-packaging).
For launcher or deployment-command changes, also use [deployment policy and launcher tests](../../../docs/verification.md#deployment-policy-and-launcher-tests).
An image build or architecture inspection does not establish JNI loading, effective OS enforcement or connectivity on a physical Pi.
