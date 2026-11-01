# Reusable TDLight library

Telegram Home Bot contains one executable Spring Boot application and one reusable library:

```text
telegram-home-bot (Spring Boot, build/libs/thb.jar)
└── implementation telegram-tdlight (ordinary library JAR)
    ├── dev.telegram.api            Public messaging API
    ├── dev.telegram.tdlight        TDLight Core
    └── dev.telegram.tdlight.linux  Linux Guard
```

The three library areas are package boundaries within one JAR. The API does not reference Core
or Linux Guard. Core requires a `NetworkEgressVerifier` supplied by its host. The application
explicitly selects `LinuxEgressVerifier`; including the library does not activate the guard.
Bot API integration, Spring wiring and business services remain in the root application.

## Dependencies and boundaries

The library uses `java-library`, Kotlin 1.9.25, JVM 17 and ktlint 1.0.1. Its production and test
classpaths exclude Spring, Boot, Actuator, TelegramBots SDK and application dependencies.
Jackson Databind 2.19.2 and the TDLight SDK are `implementation` dependencies; SDK types stay
off the application compile classpath. Native artifacts are `runtimeOnly` dependencies.

Pinned TDLight versions are Java `3.5.9+td.1.8.68`, API `4.0.565` and natives `4.0.597`.
Packaged native classifiers are `linux_arm64_gnu_ssl3` and `windows_amd64`.

`gradle/telegram-boundaries.gradle` checks the two-project dependency graph, library main/test
classpaths, API/Core package boundaries, application references and SDK isolation in the root.
It also rejects library test/check task dependencies on root application tasks.

## Public API and lifecycle

- `TdlightClient` implements `TelegramMessaging` and `AutoCloseable`, exposing `start`, `stop`,
  `close`, `isRunning` and `state`. Each instance is single-use: create a new instance after
  stop or failure. Start/stop are synchronized; repeated stop is safe. Start returns after
  native initialization; authorization and readiness complete asynchronously. States are
  `STOPPED`, `INITIALIZING`, `DISCONNECTED`, `READY` and `FAILED`.
- `TdlightClientConfig` holds the API ID/hash, session path, proxy, nullable `defaultChatId`
  and tuning. `TdlightBotCredentials` holds the token; `MtprotoProxyConfig` holds the host,
  port and full FakeTLS secret. Credential classes avoid generated secret-bearing `toString` methods.
- `TdlightTuning` defaults to a 60-second request timeout, a 5-second startup retry interval,
  and outgoing/ACK/control/event capacities of 100/16/1024/100. Native Close and factory cleanup
  have separate 20/10-second limits.
- `NetworkEgressVerifier.verify(ProxyEndpoint): VerifiedProxyEndpoint` is mandatory.
  There is no permissive default verifier, automatic unguarded mode or Bot API fallback.
- `TelegramEventHandler` receives neutral events with user, chat, message/callback IDs and time.
  Callback timestamps represent receipt time because the native update has no creation timestamp.
  Outgoing messages, anonymous/chat senders and unsupported content are filtered out.
- `OutgoingTelegramMessage` accepts a chat ID, text, optional reply target and keyboard.
  Keyboards are `TelegramKeyboard.Reply(rows)` or `Inline(rows)` with `TelegramButton(text, data)`.
  `send`, `edit` and `answerCallback` return `CompletableFuture<TelegramRequestResult>`.
  Editing supports text and inline keyboards. Send and edit reject blank text before queue admission.
- `onFailure(TdlightFailure)` reports a terminal stage without native exceptions or secrets.
  Core closes queues and starts bounded cleanup. A synchronous startup failure also notifies
  the host and throws a sanitized exception. The library does not require the host JVM to exit.
- `onDelivery(TelegramDeliveryEvent)` reports separate TDLib send-success/send-failure updates.
  Failure and delivery listeners run on transport threads and must remain short and nonblocking.
  The business event handler runs on a separate worker with a bounded queue.

### Request completion and delivery

`TelegramRequestRejectedException` means the request was not submitted to native code, for example
because of validation, a full/closed queue or a missing default chat. `TelegramRequestFailedException`
means an error or timeout after submission; delivery may remain uncertain. Only `errorCode` is exposed,
not native error text. A request result may contain a temporary native message ID.

`TelegramDeliveryEvent.Sent` represents `UpdateMessageSendSucceeded`, with old/new message IDs.
It confirms server-side sending, not recipient reading. `Failed` represents a separate delivery update.
Delivery events and request futures are independent. There is no durable queue, exactly-once guarantee
or automatic retry of an ambiguous send. TDLib owns network retries for submitted sends.

Only submitted requests have a deadline; startup buffering does not consume that timeout.
Outgoing requests use FIFO ordering, separate ACK capacity and startup retries with exponential backoff.
A timeout retains the native capacity reservation until TDLib responds. Cancelling a queued request
can free its slot; cancelling a submitted request cannot undo native delivery.

### Completion callbacks and threading

Each client publishes admitted request outcomes through a daemon `ForkJoinPool`. Native, control,
timeout and shutdown threads schedule completion rather than execute user continuations themselves.
`close()` does not wait for those continuations, so a request future may complete after it returns.

Outgoing/ACK admission accounts for both native work and completion callbacks: a slot is released
after the native response and publication, including synchronous callbacks, have completed.
Slow callbacks therefore apply backpressure. Normally the pool uses up to two workers; managed
`CompletableFuture.get/join` waits allow compensation up to
`min(32767, outgoingCapacity + acknowledgementCapacity + 2)` workers.

Use async stages with a host-owned executor for blocking I/O or lengthy work. Cyclic waits and
callbacks that never return remain host errors. Cleanup does not wait for them or forcibly stop
user code; daemon workers terminate after callbacks return. Continuations attached to an already
completed future may run on the caller thread; cancellation may also execute callbacks on its caller.

### Optional default chat

Without `defaultChatId`, the client verifies bot identity and becomes READY when connected without
calling `GetChat`. Use explicit chat IDs with `send`; `sendToDefault` rejects before submission.
With a default chat, notifications and readiness wait for `GetChat` resolution and ID verification.
Explicit sends wait for bot identity and retain FIFO ordering.

## Telegram Home Bot adapter

Root `MtprotoProperties` owns Spring configuration. `TdlightConfig` composes immutable library
configuration, credentials, `LinuxEgressVerifier`, the handler and the application's fatal policy.
`ObjectProvider<TelegramMessageHandler>` resolves the handler at event delivery to avoid a sender/handler cycle.

Root `TdlightTransport` implements `TelegramMessageSender`, Spring lifecycle and the
`tdlightTransport` health indicator. Initializing/disconnected reports `OUT_OF_SERVICE`, ready
reports `UP`, and stopped/failed reports `DOWN`. Health does not prove a Telegram round trip.

The adapter builds the current reply keyboard from `CommandService` for plain-text sends;
menu sends/edits use explicit inline keyboards. Core has no command knowledge. Cancellation of
the adapter's future propagates to Core. Authorization, blocking, deduplication, menus, WoL and
command execution remain in the application. Callback ACK admission precedes business queue
admission; if the ACK queue is full or closed, the callback event is dropped with a dedicated warning.
An admitted ACK is not awaited before dispatching the event, and a native error or timeout does not
retry the ACK or replay the action. `consumeAcknowledged` preserves authorization without sending a second ACK.

| Configuration | Selected sender | Native initialization |
|---|---|---|
| `enabled=false` | `MockTelegramMessageSender` | None |
| `enabled=true`, `transport=bot-api` (default) | `DefaultTelegramMessageSender` | None |
| `enabled=true`, `transport=mtproto` | Root `TdlightTransport` adapter | After verifier acceptance in `start` |

The application requires a notification chat and valid token even though the library supports
an optional default destination. See the [access model](telegram-mtproto.md#access-model-both-transports).
A terminal application callback requests exit code 1, with graceful JVM/Spring hooks, an independent
55-second `halt(1)` watchdog and Docker's `unless-stopped` restart policy.

## Linux Guard and trust boundary

`LinuxEgressVerifier` checks root ownership, absence of symlinks and group/other write permissions
for the launch record and its ancestors. It verifies `/proc` identity, all UID fields, boot ID,
PID, `NoNewPrivs`, `Seccomp=2`, capability sets allowing only `NET_RAW`, socket policy version **3**,
the requested proxy host/port and a numeric pinned IP. Defaults are `/run/thb/egress.json` and UID 10001.
Custom path/UID values require matching launcher and OS policy; changing verifier settings does
not configure enforcement.

Verification runs before session/native factory creation. Core checks requested endpoint identity,
uses the verified numeric address and original port in `AddProxy`, and verifies enabled proxy
readback with the full secret before enabling networking. Failure stops startup without fallback.

Core validates and normalizes numeric addresses without DNS before session/factory creation.
IPv4 requires four decimal octets without leading zeros; IPv6 is normalized through the JDK,
including IPv4-mapped IPv6 conversion to IPv4. Hostnames, malformed literals, brackets and zone IDs
are rejected. The standard Linux verifier accepts a narrower set of IPv6 representations.
A verifier must attest effective policy for the current process, without reusing attestations
across processes or policy changes. Numeric validation does not prove firewall enforcement.

**The Kotlin verifier does not install a firewall.** It trusts the protected root-owned attestation
from `deploy/telegram-entrypoint.py`, which installs/verifies nftables rules, applies seccomp policy
v3, drops privileges and executes Java. Process-wide policy includes DNS/LAN/weather exceptions;
it does not sandbox a malicious root administrator or arbitrary host application code.
A host providing its own verifier is responsible for effective OS enforcement.

Core builds/tests on Windows and Linux using test verifiers. The production Linux verifier
rejects unsupported platforms before JNI. Packaged natives support Windows AMD64 and Linux ARM64
GNU/OpenSSL3; packaging Windows natives does not enable unguarded Windows MTProto login.
See the [transport runbook](telegram-mtproto.md) for launcher, storage and deployment requirements.

## Build and reuse

See [developer verification](verification.md) for commands and their limits.
Public contracts are in `telegram-tdlight/src/main/java/dev/telegram/api`; Linux Guard is in
`telegram-tdlight/src/main/java/dev/telegram/tdlight/linux`; library tests are under
`telegram-tdlight/src/test/java`.

The [standalone consumer](../samples/tdlight-consumer/README.md) includes only the library source
project, with independent build outputs. It demonstrates configuration, handler registration,
explicit send, failure/state and close with a deliberately rejecting verifier before JNI.
It proves source-project reuse, not Maven publication independence or Telegram connectivity.
A real consumer must supply effective OS enforcement; no verifier is selected automatically.

See [verification status and limits](verification.md#recorded-verification-baseline) for the dated
automated baseline. Physical Pi startup, Telegram/telemt round trips and soak testing require
separate verification using the transport runbook.
