---
name: spring-boot
description: "Use for Spring Boot work in telegram-home-bot: configuration, scheduling, security, Thymeleaf, Telegram bot, OpenWeather, actuator, events, testing."
---

# Spring Boot Skill

## Context
This project uses **Spring Boot 3.5.4** with Kotlin. Key starters: Web, WebFlux, Data JPA, Security, Validation, Actuator, Thymeleaf, DevTools.

## Configuration

### Properties & YAML
- Primary config: `src/main/resources/application.yaml`
- Environment variables via `spring-dotenv` (me.paulschwarz:spring-dotenv:4.0.0)
- Custom `@ConfigurationProperties` classes: `BotProperties`, `OpenWeatherProperties`
- `@Value` for simple property injection with defaults: `@Value("\${bcrypt.rounds}")`
- Relaxed binding: `WAKE_ON_LAN_BROADCAST_IP` → `wakeOnLan.broadcast.ip`

### Scheduling
```kotlin
@Configuration
@EnableScheduling
class TaskConfig

@Component
class BroadcastPingScheduler {
    @Scheduled(fixedRateString = "\${network-monitor.broadcast.ping.scheduled-time}")
    fun runBroadcastPing() { /* ... */ }
}
```
- Always use property placeholders for `@Scheduled` rates
- `fixedRateString` and `initialDelayString` with property references

### Conditional Beans
```kotlin
@Service
@ConditionalOnProperty(name = ["integrations.telegram.enabled"], havingValue = "true")
class DefaultTelegramMessageConsumer(/* ... */)
```
- Telegram integration components are gated behind `integrations.telegram.enabled`
- Weather components behind `integrations.openweather.enabled`

## Web Layer

### Controllers
- `@Controller` + Thymeleaf for HTML views
- `@RequestMapping` with constants from `AppConstants`
- `@PathVariable`, `@RequestParam` for parameter binding
- Redirect pattern: `"redirect:$MAPPING"` using `AppConstants`

### Thymeleaf
- Layout dialect: `thymeleaf-layout-dialect` with `layout.html` decorator
- Spring Security integration: `thymeleaf-extras-springsecurity6`
- Templates: `src/main/resources/templates/`
- Static resources: `src/main/resources/static/` (favicon, CSS, JS)
- WebJars for Bootstrap 5.1.1, jQuery 3.6.0, Font Awesome 5.15.3

### Actuator
```yaml
management:
  endpoints.web.exposure.include: '*'
  endpoint.health.show-details: when_authorized
```
- Custom health indicator: `CommandService` implements `HealthIndicator`
- Custom endpoint: `DbActuatorEndpoint` for DB info
- Actuator endpoints require `ROLE_ADMIN`

## Security

### Spring Security
- `@EnableWebSecurity` + `@EnableMethodSecurity`
- BCrypt with configurable rounds (default: 11)
- `DaoAuthenticationProvider` with custom `AppUserDetailsService`
- `SimpleAuthorityMapper` with uppercase conversion and default `ROLE_USER`
- Form login, CSRF disabled, frame options disabled (H2 console)
- Admin-only paths: `/commands/edit/**`, `/hosts/edit/**`, `/actuator/**`

### Auth Entities
- `User` → `Role` → `Privilege` (many-to-many chain)
- `LoginAttemptService` for brute-force protection (block after 5 failures, 600 min)
- `AuthenticationFailureListener` / `AuthenticationSuccessEventListener`
- Password validation via Passay + custom `@ValidPassword`

## Integration Patterns

### Telegram Bot
- `telegrambots-springboot-longpolling-starter:9.0.0`
- `LongPollingSingleThreadUpdateConsumer` interface
- `BotStarter` handles registration with conditional enablement
- Mock sender (`MockTelegramMessageSender`) for testing

### OpenWeather API
- Spring WebFlux `WebClient` with configurable base URL
- `WeatherClientConfig` creates `WebClient` bean with timeout
- `WeatherService` calls API and maps to `WeatherResponse`

### Event-Driven
- Spring Application Events: `ScanHostsEvent`
- `ScanHostsEventPublisher` + `ScanHostsEventListener` (async with `@EventListener`)

## I18n
- `messages.properties` (English) / `messages_ru.properties` (Russian)
- `CookieLocaleResolver` with `localeChangeInterceptor` (param `?lang=`)
- `MessageService` / `MessageFormatService` wrap `MessageSource`
- `LocalValidatorFactoryBean` with message source for validation i18n

## Do NOT
- Do NOT use `@Autowired` — always constructor injection
- Do NOT hardcode property values — use `@Value` or `@ConfigurationProperties`
- Do NOT use XML-based Spring config — annotations only
- Do NOT add new endpoints without updating `SecurityConfiguration` if they need admin protection
- Do NOT disable Flyway — schema changes always via migrations
