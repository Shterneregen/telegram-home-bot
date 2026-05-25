---
name: docker
description: "Use for Dockerfile and container runtime changes in telegram-home-bot: Raspberry Pi/Linux runtime, fping/iproute2, Gradle bootJar, network mode, ports, logs."
---

# Docker Skill

## Context
This project has a single [`Dockerfile`](Dockerfile) for building a container image. The application is designed to run on Raspberry Pi (ARM) and Linux.

## Dockerfile

```dockerfile
FROM openjdk:11-jre-slim-buster
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}
WORKDIR application
COPY build/libs/*.jar app.jar
RUN apt-get update && \
    apt-get -y install fping &&  \
    apt-get -y install iproute2 &&  \
    apt-get clean
ENTRYPOINT ["java", "-jar","./app.jar"]
```

## Build & Run Commands

```bash
# Build JAR first
./gradlew clean bootJar

# Build Docker image
docker build -t thb-image .

# Run container (bridge network — host network scanning works)
docker run -d -p 80:8080 --network=bridge --name=thb thb-image

# Start/stop
docker start thb
docker stop thb

# View logs
docker logs -f thb
```

## Key Notes

### System Dependencies
The image installs:
- **fping** — required for broadcast ping network scanning
- **iproute2** — required for `ip -j n show` (ARP table queries)

### Network Mode
- Use `--network=bridge` for container-to-host network visibility
- Host network mode (`--network=host`) can also be used for full LAN access

### Port Mapping
- Container exposes port **8080** (overridden from default 9988 via `SERVER_PORT=8080`)
- Map to any host port: `-p 80:8080` in examples

### Java Version
- Dockerfile uses **OpenJDK 11** (JRE slim), but the project compiles with **JDK 17**
- Ensure compatibility or update the base image if needed

## Do NOT
- Do NOT add new system packages to Dockerfile without clear necessity
- Do NOT change the `WORKDIR` from `application` without updating `COPY` and `ENTRYPOINT`
- Do NOT remove `apt-get clean` — it reduces image size
- Do NOT hardcode the JAR filename — the wildcard `*.jar` is intentional
