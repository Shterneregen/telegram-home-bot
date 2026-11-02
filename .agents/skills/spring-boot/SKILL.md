---
name: spring-boot
description: "Change THB application bean wiring, configuration, scheduling, MVC/Thymeleaf, security or Actuator. Telegram protocol logic and JPA mappings have separate skills."
---

# Spring Boot application

## Configuration and wiring

- Read [application.yaml](../../../src/main/resources/application.yaml), [.env.example](../../../.env.example) and the affected properties class together. Keep property names, environment placeholders and defaults aligned.
- Preserve optional-integration behavior: check both enabled and disabled bean graphs when changing wiring. For Telegram sender selection, use the [adapter configuration table](../../../docs/telegram-modules.md#telegram-home-bot-adapter).
- Spring wiring belongs to the root application. The reusable `telegram-tdlight` library stays independent of Spring.
- Existing WebFlux dependencies support the OpenWeather `WebClient`; their presence is not a reason to convert MVC controllers to a reactive stack.

## Web and security

- Use the existing [templates](../../../src/main/resources/templates), [static resources](../../../src/main/resources/static) and application route constants.
- Inspect [SecurityConfiguration](../../../src/main/java/random/telegramhomebot/auth/SecurityConfiguration.kt) when adding or changing routes; preserve the distinction between public health checks, admin operations and authenticated application pages.
- Actuator exposure is currently `health,info`. Health endpoints are publicly accessible for container probes, while other Actuator paths require admin authority. Coordinate any change with the Compose health check; do not restore wildcard exposure from old examples.
- Use existing `MessageService` / `MessageFormatService` and English/Russian message bundles for user-facing text.

## Verification

Choose affected application tests from [JVM verification](../../../docs/verification.md#jvm-checks-and-packaging).
For security changes, exercise relevant anonymous, authenticated and admin access; for integration wiring, check disabled mode as well as the changed enabled mode.
Library checks alone do not cover root application behavior.
