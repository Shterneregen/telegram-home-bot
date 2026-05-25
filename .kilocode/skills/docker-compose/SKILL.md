---
name: docker-compose
description: "Use when adding or changing Docker Compose for telegram-home-bot: app service, .env, persistent H2 data, SonarQube profile, bridge/host networking."
---

# Docker Compose Skill

## Context
This project currently has no committed `docker-compose.yml` in the public repository. If one is needed, create it deliberately rather than assuming it already exists. If one is needed, it should define the THB application service and potentially a SonarQube service for code quality.

## Typical Docker Compose Structure

```yaml
version: '3.8'
services:
  thb:
    build: .
    image: thb-image
    container_name: thb
    ports:
      - "80:8080"
    network_mode: bridge
    restart: unless-stopped
    env_file:
      - .env
    volumes:
      - thb-data:/application/data

  sonarqube:
    image: sonarqube:latest
    container_name: sonarqube
    ports:
      - "9000:9000"
      - "9092:9092"
    restart: unless-stopped
    volumes:
      - sonarqube-data:/opt/sonarqube/data
    profiles:
      - tools

volumes:
  thb-data:
  sonarqube-data:
```

## Key Considerations

### Network
- Use `network_mode: bridge` for LAN access (network scanning)
- For production on Raspberry Pi, consider `network_mode: host`

### Environment
- Pass all variables via `env_file: .env`
- Required variables: `TELEGRAM_TOKEN`, `TELEGRAM_BOT_CHAT_ID`
- Feature gates: `TELEGRAM_ENABLED`, `NETWORK_MONITOR_ENABLED`, `OPENWEATHER_ENABLED`

### Volumes
- Mount data directory for H2 database persistence
- Mount `.env` for configuration

### Profiles
- Use `profiles: [tools]` for optional services like SonarQube
- Main app service should be in default profile (no profile needed)

### SonarQube (Optional)
```yaml
sonarqube:
  image: sonarqube:latest
  ports:
    - "9000:9000"
  profiles:
    - tools
```

## Do NOT
- Do NOT hardcode secrets in docker-compose.yml — use `.env` file
- Do NOT use `network_mode: host` unless on Raspberry Pi
- Do NOT forget to volume-mount the H2 database directory for persistence
