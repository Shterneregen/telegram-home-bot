---
name: kotlin
description: "Use for Kotlin code in telegram-home-bot: JVM 17, Spring Boot Kotlin, Gradle Groovy DSL, src/main/java Kotlin layout, null-safety, constructor DI, ktlint."
---

# Kotlin Skill

## Language Context
This project uses **Kotlin 1.9.25** targeting **JVM 17**. All source files are `.kt` located under `src/main/java/` (legacy layout — do not move them).

## Key Kotlin Conventions for This Project

### Null Safety
- Use `?.let { }` for null-safe chaining — this is the dominant pattern
- `lateinit var` for `@Value`-injected Spring properties
- `Optional<Host?>` return types are used in some repository/service methods — unwrap with `.orElse(null)`
- Never use `!!` — prefer safe calls or early returns

### Data Classes vs Regular Classes
- JPA entities are **regular classes** (not `data class`) — required by Hibernate
- DTOs like `PasswordDto`, `TimeLogDto`, `GenericResponse` are regular classes with `var`/`val`
- Use `object` for constants (`AppConstants`), enums for type-safe constants

### Expression Style
- Prefer expression bodies: `fun getHostByMac(mac: String?): Host? = hostRepository.findHostByMac(mac)`
- Use `when` as expression (exhaustive), not statement
- Use `if-else` as expression when possible

### Annotations
- `@field:NotBlank` — JSR-380 annotations require `@field:` target in Kotlin
- `@JvmStatic` — not needed in this project (no Java interop from outside)
- `@Throws(Exception::class)` — used sparingly, only in `SecurityConfiguration.filterChain()`

### Collections & Functional Style
- Use `.filter { }`, `.map { }`, `.sortedWith()`, `.joinToString()`, `.none { }`
- `ArrayList()` for mutable lists when building keyboard rows
- `Pair(a, b)` for destructuring declarations

### Logging
- `val log = logger()` from `random.telegramhomebot.utils.logger` — project utility
- Use structured logging: `log.debug("[{}] command was executed", update.message)`
- Guard debug logs with `if (log.isDebugEnabled)` for expensive formatting

### Constructors & DI
- **Constructor injection only** — no `@Autowired` on fields or setters
- Primary constructor with `private val` for injected dependencies
- Secondary constructors used in `Host` entity for convenience

## Do NOT
- Do NOT add semicolons
- Do NOT use `!!` (force unwrap)
- Do NOT use `data class` for JPA entities
- Do NOT use `@Autowired` on fields — always constructor injection
- Do NOT create interfaces with single implementation unless there's a clear strategy pattern
- Do NOT move Kotlin sources out of `src/main/java/` — it's the established layout
