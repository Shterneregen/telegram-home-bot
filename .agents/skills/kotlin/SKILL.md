---
name: kotlin
description: "Apply THB-specific Kotlin source layout, Gradle configuration and logging conventions when editing Kotlin code or its build. Framework and persistence behavior have separate skills."
---

# Kotlin project conventions

## Layout and build

- Preserve the existing `src/main/java` Kotlin layout in the root application and `telegram-tdlight`; inspect each project's source sets before adding files.
- Read versions and compiler/plugin settings from [gradle.properties](../../../gradle.properties), [root build](../../../build.gradle) and, when relevant, [library build](../../../telegram-tdlight/build.gradle).
- The application uses `random.telegramhomebot`; the reusable library uses `dev.telegram`. For cross-module changes, read [dependency boundaries](../../../docs/telegram-modules.md#dependencies-and-boundaries). Do not pull application or Spring dependencies into the library.

## Local idioms

- In the application, use `logger()` from `random.telegramhomebot.utils.logger` and parameterized messages. Keep expensive formatting conditional and avoid logging credentials or complete Telegram updates.
- The library has its own logging setup; do not import the application's utility.
- When touching legacy Java `Optional` APIs, unwrap explicitly at the boundary; do not spread nested nullable/Optional types into new APIs.
- Follow the surrounding DTO and API contracts rather than copying incidental choices such as mutable lists or regular DTO classes everywhere.

## Verification

Select affected tests, lint and packaging checks from [developer verification](../../../docs/verification.md#jvm-checks-and-packaging).
For module or dependency changes, include boundary verification; for library reuse changes, consider the [independent consumer](../../../docs/verification.md#independent-source-consumer).
The recorded lint baseline is dated evidence, not a result for the current checkout. Report the checks actually run and avoid unrelated formatting churn.
