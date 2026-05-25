---
name: "raspberry-pi-deploy"
description: "Use when changing Docker, Docker Compose, runtime configuration, Raspberry Pi deployment, Linux service setup, or lightweight production deployment."
---

# Raspberry Pi Deploy Skill

Use this skill for deployment and runtime changes targeting Raspberry Pi or small Linux servers.

## Project Context

Telegram Home Bot is intended to run as a lightweight home automation service on Raspberry Pi or similar Linux hosts.

Deployment concerns:

- Docker image size
- JVM memory usage
- Persistent H2 database storage
- `.env` configuration
- Network access for scanning and Wake-on-LAN
- Restart behavior
- Logs and troubleshooting

## Rules

- Keep the deployment simple and reproducible.
- Prefer Docker Compose for local home-server deployment.
- Keep secrets in `.env`, not in committed files.
- Do not commit Telegram tokens or OpenWeather API keys.
- Mount persistent storage for the H2 database.
- Keep JVM memory settings suitable for Raspberry Pi.
- Avoid unnecessary heavy base images.
- Document required host network permissions when network scanning or Wake-on-LAN needs them.

## Docker Recommendations

- Use a lightweight JRE image when possible.
- Expose only the required application port.
- Use environment variables for configuration.
- Add a healthcheck if the application exposes an actuator health endpoint.
- Store database files in a mounted volume.

## Docker Compose Checklist

When editing `docker-compose.yml`:

1. Keep service name stable.
2. Configure restart policy, usually `unless-stopped`.
3. Use `.env` for tokens and feature flags.
4. Mount persistent data directory.
5. Map the application port explicitly.
6. Consider `network_mode: host` only when required for LAN scanning or Wake-on-LAN.
7. Document trade-offs of host networking.

## Example Runtime Concerns

For Raspberry Pi:

- Keep memory usage low.
- Avoid frequent heavy scanning.
- Make scan intervals configurable.
- Ensure logs do not grow indefinitely.
- Prefer graceful restart and automatic recovery.

## Security Notes

- Do not expose the web UI to the public internet without authentication and HTTPS.
- Do not log secrets.
- Keep `.env` excluded from Git.
- Treat Telegram chat IDs and user IDs as sensitive configuration.
