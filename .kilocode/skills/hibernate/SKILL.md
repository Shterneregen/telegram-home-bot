---
name: hibernate
description: "Use for Hibernate/JPA/Flyway work in telegram-home-bot: H2 schema migrations, Kotlin entities, repositories, lazy relations, sequences, entity equality."
---

# Hibernate / JPA Skill

## Context
This project uses **Hibernate 6.x** (managed by Spring Boot 3.5.4) with **H2 embedded database**. Entities are in `random.telegramhomebot.db.model` and `random.telegramhomebot.auth.db.entities`. Flyway manages schema migrations.

## Entity Conventions

### ID Generation
- Use `GenerationType.SEQUENCE` with `@SequenceGenerator` and `allocationSize = 1`
- Sequence naming: `host_seq`, `host_time_log_seq`, `users_seq`
- `@Column(updatable = false, nullable = false)` on ID fields

### Entity Structure
```kotlin
@Entity
@Table(name = "host")
class Host(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "host_seq")
    @SequenceGenerator(name = "host_seq", sequenceName = "host_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    var id: Long? = null,
    // ... fields
)
```

### Nullable Fields
- All entity fields use nullable types (`String?`, `Long?`, `Boolean?`, `List?`)
- `id` is `Long?` (null before persistence)
- Validation annotations use `@field:` target: `@field:NotBlank(message = "{...}")`

### Relationships
- `@OneToMany(fetch = FetchType.LAZY, mappedBy = "host", cascade = [CascadeType.REMOVE])`
- Always use `FetchType.LAZY` for collections
- Cascade types: `REMOVE` for parent-child relationships

### equals/hashCode
- Override `equals` and `hashCode` on natural keys (e.g., `mac` field in `Host`)
- Do NOT rely on `id` equality (null before persist)
- Example:
```kotlin
override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val host = other as Host
    return mac == host.mac
}
override fun hashCode(): Int = mac?.hashCode() ?: 0
```

## Repository Conventions

### Interface Declaration
```kotlin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HostRepository : JpaRepository<Host, Long> {
    fun findHostByMac(mac: String?): Host?
    fun findAllByIpNotNull(): List<Host>
    fun findReachableHosts(): List<Host>
}
```

### Custom Queries
- Use Spring Data derived query methods where possible
- Use `@Query` with JPQL for complex queries:
```kotlin
@Query("select h from Host h where h.state = 'REACHABLE'")
fun findReachableHosts(): List<Host>
```

## Database Configuration

### application.yaml
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:h2:${user.dir}/thb}
    driverClassName: org.h2.Driver
  jpa:
    hibernate.ddl-auto: validate  # Flyway manages schema, Hibernate validates
    database-platform: org.hibernate.dialect.H2Dialect
    show-sql: false
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
```

### Migrations
- Flyway SQL files: `src/main/resources/db/migration/V*__Description.sql`
- Naming: `V{major}.{minor}__Description.sql`
- Existing migrations: `V1.1__Init_DB.sql`, `V1.2__auth_tables.sql`

## Do NOT
- Do NOT use `data class` for entities — Hibernate proxy requires regular classes
- Do NOT use `@ManyToOne(fetch = FetchType.EAGER)` — always LAZY
- Do NOT use `GenerationType.IDENTITY` — use SEQUENCE
- Do NOT modify entities without creating corresponding Flyway migration
- Do NOT set `ddl-auto` to `create` or `update` — Flyway manages schema
