---
name: hibernate
description: "Change THB JPA entities, repository queries, fetch behavior or entity equality. Use flyway when the persisted schema or migration history is involved."
---

# Hibernate and JPA

## Sources

Entities live in [db/model](../../../src/main/java/random/telegramhomebot/db/model) and [auth/db/entities](../../../src/main/java/random/telegramhomebot/auth/db/entities).
Inspect the affected repository and its service callers alongside the entity.
Database settings are in [application.yaml](../../../src/main/resources/application.yaml); schema history is in [migrations](../../../src/main/resources/db/migration).

## Mapping decisions

- Keep regular classes for JPA entities as the project convention. Avoid generated equality, hashing or string rendering that traverses relationships.
- Existing generated IDs use nullable `Long` and sequences. Match each generator's name and allocation size to its actual database sequence.
- Choose property nullability from the domain contract and schema; the project contains both nullable and non-nullable fields. Use `@field:` for field-based Bean Validation.
- Choose fetch and cascade behavior per relationship and caller. Prefer lazy loading where suitable, but account for transaction boundaries and existing auth loading. Do not apply `REMOVE` cascade to shared entities by copying a parent-child example.
- Before changing equality, inspect collection and proxy usage. Use a natural key only when its uniqueness and stability are guaranteed; account for transient entities if using generated IDs. Do not copy mutable-key equality as a universal template.
- Keep repository queries consistent with mappings and service transaction boundaries. Check the actual access pattern when addressing lazy loading or excess queries.
- Add a Flyway migration when the persisted schema changes, not for every entity edit.

## Verification

Run the affected persistence tests using [JVM verification](../../../docs/verification.md#jvm-checks-and-packaging).
For fetch, cascade or equality changes, verify the relevant load/delete/collection behavior, including transient and persisted entities where applicable.
