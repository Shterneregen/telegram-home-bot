---
name: "flyway"
description: "Use when changing database schema, writing Flyway migrations, reviewing SQL migrations, or working with H2 and Spring Boot database startup."
---

# Flyway Skill

Use this skill for database schema changes in Telegram Home Bot.

## Project Context

- The project uses Spring Boot with H2 and Flyway.
- Migrations are stored in `src/main/resources/db/migration`.
- Database changes must be made through Flyway SQL migrations.
- Do not rely on Hibernate auto-DDL for persistent schema changes.

## Rules

- Create new migrations instead of editing already applied migrations.
- Use clear migration names:
  - `V1__Initial_schema.sql`
  - `V2__Add_feature_switcher.sql`
  - `V3__Add_weather_location.sql`
- Keep migrations small and focused.
- Prefer explicit constraints and indexes.
- Make migrations repeatable only when the content is intentionally regenerated.
- Keep SQL compatible with H2 unless the project explicitly moves to another database.
- Do not delete existing data unless the task explicitly requires it.

## Checklist

Before changing database code:

1. Check the related JPA entity.
2. Check the repository queries.
3. Add or update a Flyway migration.
4. Make sure column names match entity mappings.
5. Consider indexes for frequently searched fields.
6. Run tests or at least verify application startup.

## Common Tasks

### Add a new field to an entity

1. Add the Kotlin property to the JPA entity.
2. Add a Flyway migration with `ALTER TABLE`.
3. Update forms, DTOs, templates, or services if needed.
4. Add a default value when the column is non-nullable.

### Add a new table

1. Create a migration with `CREATE TABLE`.
2. Add primary key and required constraints.
3. Add indexes for lookup columns.
4. Add a JPA entity.
5. Add a Spring Data repository.
