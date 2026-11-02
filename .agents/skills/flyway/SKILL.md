---
name: flyway
description: "Create or review THB H2 schema migrations and diagnose Flyway validation or migration startup failures. Entity-only mapping and query work belong to hibernate."
---

# Flyway migrations

## Sources

Inspect the current [migrations](../../../src/main/resources/db/migration), affected entity mappings and repository queries.
Read database settings from [application.yaml](../../../src/main/resources/application.yaml); do not copy them into a separate recipe.

## Migration decisions

- Add a new versioned migration for schema changes; do not edit already applied migrations. Inspect existing versions before choosing the next unique version and follow their naming convention.
- Keep SQL compatible with the project's H2 version. Match table, column, constraint and sequence names to the mappings, including sequence increments and allocation sizes.
- Preserve existing data unless its removal is part of the task. For a required column on an existing table, choose a meaningful backfill or default before enforcing `NOT NULL`; do not invent a domain value just to make migration pass.
- Add indexes for actual query needs and constraints for required invariants.
- Use repeatable migrations only for objects intentionally recreated on reapplication, not to revise versioned migration history.
- A Kotlin behavior or mapping-only edit does not automatically require SQL; determine whether the persisted schema changes.

## Verification

For a schema change, verify migration of both an empty H2 database and a representative prior schema with data, using disposable databases.
Check Hibernate schema validation after migration and any affected repository behavior.
Use [JVM verification](../../../docs/verification.md#jvm-checks-and-packaging) for project commands; ordinary unit tests alone do not establish upgrade compatibility.
