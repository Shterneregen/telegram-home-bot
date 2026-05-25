---
name: "telegram-bot"
description: "Use when changing Telegram bot commands, message handling, menus, callbacks, notifications, or Telegram integration in Telegram Home Bot."
---

# Telegram Bot Skill

Use this skill for Telegram integration changes in Telegram Home Bot.

## Project Context

The project uses Telegram bot integration for home automation commands, notifications, Wake-on-LAN, weather, and feature menus.

Important areas:

- `integrations.telegram`
- `integrations.telegram.consume`
- `integrations.telegram.send`
- `integrations.telegram.menu`
- `services.messages`
- `services.commands`
- `services.UserValidatorService`

## Rules

- Validate user access before executing Telegram commands.
- Keep Telegram message handling thin.
- Put business logic into services.
- Keep menu and callback logic readable and explicit.
- Use existing message formatting and i18n services where possible.
- Do not hardcode user-facing text if the project already uses message bundles.
- Do not log Telegram tokens, chat IDs, or user-sensitive data.
- Respect `integrations.telegram.enabled` / `TELEGRAM_ENABLED`.
- Keep mock sender support for local development and tests.

## Message Handling Flow

Expected flow:

1. Receive update.
2. Validate user.
3. Parse command or callback.
4. Delegate to service.
5. Format response.
6. Send response through Telegram sender.

## Checklist

When adding a new Telegram command:

1. Add command parsing or callback handling.
2. Check authorization.
3. Add business logic in a service.
4. Add response formatting.
5. Add i18n message keys if needed.
6. Add tests where practical.
7. Verify behavior when Telegram integration is disabled.

## Callback Menus

- Keep callback data short and stable.
- Avoid putting sensitive data into callback payloads.
- Prefer explicit callback prefixes for feature areas.
- Handle unknown or outdated callbacks gracefully.

## Notifications

- Avoid duplicate notifications.
- Keep notification text concise.
- Include enough context for the user to act.
- Do not block scheduled jobs on Telegram send failures unless required.
