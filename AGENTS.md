# AGENTS.md

## Project Overview

Telegram Home Bot (THB) is a Spring Boot + Kotlin application for home automation and Telegram-based remote control.

Main features:
- Telegram bot integration
- Home network monitoring
- Wake-on-LAN
- Remote command execution
- Weather integration via OpenWeatherMap
- Web UI with Thymeleaf

Stack:
- Kotlin 1.9
- Spring Boot 3.5
- Spring Security
- Spring Data JPA / Hibernate
- H2 + Flyway
- Docker / Docker Compose
- Thymeleaf + Bootstrap

## Architecture

Layered architecture:
- Controllers handle HTTP and Telegram entrypoints
- Services contain business logic
- Repositories handle data access
- Entities define JPA models

Main modules:
- `auth` — authentication and security
- `integrations.telegram` — Telegram bot integration
- `integrations.openweather` — weather API integration
- `monitor` — network scanning and scheduling
- `services.commands` — remote command execution
- `services.csv` — CSV import/export

## Kotlin Conventions

- Prefer constructor injection
- Use expression bodies for simple methods
- Prefer immutable `val`
- Use null-safety idioms: `?.let`, Elvis operator
- Avoid unnecessary `lateinit`
- Keep functions small and focused

## Spring Boot Conventions

- Use `@ConfigurationProperties` for grouped settings
- Keep controllers thin
- Put business logic into services
- Use `@ConditionalOnProperty` for optional integrations
- Keep scheduled jobs configurable via properties
- Use Flyway for all database schema changes

## Database

- H2 embedded database
- Flyway migrations are stored in:
  `src/main/resources/db/migration`
- Data access is implemented with Spring Data JPA / Hibernate

## Docker

Build:

```bash
./gradlew clean bootJar
docker build -t thb-image .
```

Run:

```bash
docker run -d -p 8080:8080 thb-image
```

## Important Environment Variables

- `TELEGRAM_ENABLED`
- `TELEGRAM_TOKEN`
- `NETWORK_MONITOR_ENABLED`
- `OPENWEATHER_ENABLED`
- `OPENWEATHER_APPID`
- `DB_URL`

## Notes For AI Agents

- Preserve the existing package structure
- Do not introduce unnecessary abstractions
- Prefer Spring Boot idioms over custom frameworks
- Keep compatibility with JVM 17
- Prefer incremental refactoring
- Keep the Docker image lightweight
- Do not replace Flyway migrations with Hibernate auto-DDL
