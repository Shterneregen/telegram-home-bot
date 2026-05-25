# Project Rules

- Use Kotlin and Spring Boot idioms.
- Prefer constructor injection.
- Keep controllers thin.
- Put business logic into services.
- Use Spring Data JPA repositories for persistence.
- Use Flyway migrations for schema changes.
- Do not use Hibernate auto-DDL for production changes.
- Keep JVM 17 compatibility.
- Do not introduce reactive stack unless explicitly requested.
- Do not replace Thymeleaf UI with SPA frameworks unless explicitly requested.
- Preserve existing package structure: `random.telegramhomebot`.
- For Docker changes, keep the Raspberry Pi deployment scenario in mind.