# Independent offline consumer

From the repository root with JDK 17:

```shell
./gradlew -p samples/tdlight-consumer offlineSmoke --offline
```

Dependencies must already be cached for Gradle offline mode. This separate build includes only the `telegram-tdlight` source project (API/Core/Linux Guard in one JAR),
uses independent build outputs and asserts absence of Spring/Bot API SDK dependencies. No THB imports,
credentials or network are used. The deny-all verifier intentionally exercises startup failure before JNI,
rejects a buffered explicit-chat send, reports terminal failure and closes the client idempotently.
The handler is registered but no incoming events are fabricated here; core tests exercise incoming events
and successful lifecycle with mocked native dependencies.

For actual use supply a verifier backed by OS policy, real bot credentials and an event handler; start,
observe state, send explicit messages, then close. Never replace the offline verifier with a permissive
production stub. See [library architecture and API](../../docs/telegram-modules.md) for result semantics
and Linux security requirements, and [developer verification](../../docs/verification.md) for the full workflow.

This verifies source-project reuse without the root application build. Maven publication independence
is a separate stage and is not proven by this sample. Guard is not activated automatically.
