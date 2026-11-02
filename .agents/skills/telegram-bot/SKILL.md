---
name: telegram-bot
description: "Change THB Telegram commands, callbacks, notifications or Bot API/MTProto transport behavior, including the reusable telegram-tdlight library and application adapters."
---

# Telegram integration

## Read according to the task

- For command, callback or authorization changes, read the [access model](../../../docs/telegram-mtproto.md#access-model-both-transports) and inspect [TelegramMessageHandler](../../../src/main/java/random/telegramhomebot/integrations/telegram/consume/TelegramMessageHandler.kt).
- For transport adapters or library changes, read [module boundaries](../../../docs/telegram-modules.md#dependencies-and-boundaries) and the relevant [API/lifecycle](../../../docs/telegram-modules.md#public-api-and-lifecycle) sections.
- For native startup, proxy or session changes, read [guarded startup](../../../docs/telegram-mtproto.md#guarded-linux-startup) and [storage and runtime](../../../docs/telegram-mtproto.md#storage-and-runtime).

## Contracts to preserve

- Keep business authorization, deduplication, menus and command execution in the application. Both transports feed the shared handler through neutral events; SDK types remain inside their transport boundary.
- Authorize the immediate sender's user ID, not the destination chat ID. Preserve the documented allowlist and login-attempt blocking for both text and callbacks.
- Preserve callback ACK semantics: MTProto acknowledges before business queue admission, then uses `consumeAcknowledged` to avoid a second ACK while retaining authorization.
- Route replies to the source chat and notifications to the configured destination. Use existing message formatting and i18n services.
- Preserve mock sender selection when Telegram is disabled and explicit Bot API/MTProto selection when enabled. Do not add an automatic fallback around guarded MTProto startup.
- For send/retry changes, read [request completion and delivery](../../../docs/telegram-modules.md#request-completion-and-delivery). Request completion is distinct from delivery; an ambiguous send must not be blindly retried.
- For listener or shutdown changes, read [completion callbacks and threading](../../../docs/telegram-modules.md#completion-callbacks-and-threading); keep transport callbacks short and nonblocking.
- Keep callback payloads stable and free of secrets; handle unknown or stale callbacks. Do not log tokens, proxy secrets, chat IDs or complete user messages.

## Verification

Use the relevant application/library checks from [developer verification](../../../docs/verification.md#jvm-checks-and-packaging).
For handler changes, cover unauthorized text/callbacks and deduplication; for selection changes, cover disabled, Bot API and MTProto modes.
Native startup and Telegram round trips require the separate [manual acceptance checks](../../../docs/telegram-mtproto.md#manual-verification-test-bot-first); unit tests or packaging do not establish them.
