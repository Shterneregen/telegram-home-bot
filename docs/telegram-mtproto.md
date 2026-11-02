> Reusable library: see [architecture and API](telegram-modules.md) and [developer verification](verification.md). Spring adapters live in the application; the pure core requires an explicit egress verifier.

# Telegram transports

`TELEGRAM_TRANSPORT=bot-api` is the default. `TELEGRAM_ENABLED=false` disables both transports.
The MTProto implementation uses TDLight 3.5.9+td.1.8.68 (API 4.0.565, natives 4.0.597), Java 17,
and the existing external telemt server. No telemt, SOCKS5, VPN or additional service runs on the Pi.

The implementation, SDK-free contracts, Linux Guard and all library tests live in the single
`telegram-tdlight` library. The root application owns conditional Spring wiring and produces the single
`thb.jar`, including the library and native dependencies. See [module architecture](telegram-modules.md).

## Guarded Linux startup

TDLib ConfigRecoverer can open direct connections independently of AddProxy. The Docker entrypoint
therefore installs a scoped nftables OUTPUT allowlist before Java starts, then uses Linux setuid/capset/prctl to run Java
as reserved UID/GID **10001**, with no-new-privileges and only NET_RAW (for fping). NET_ADMIN is removed
from all capability sets. Before dropping privileges the launcher installs an inherited libseccomp filter:
AF_PACKET, AF_XDP, legacy SOCK_PACKET, non-ICMP IP raw sockets and io_uring_setup return EPERM.
This closes the packet-socket bypass of inet OUTPUT while retaining raw ICMP for fping. The JVM and
all its children inherit the filter across exec and cannot remove it. Bot API uses the original JVM launch.

Use `deploy/docker-compose.mtproto.yml` together with the base Compose file. It grants NET_ADMIN only to the
initial root launcher. Without this privilege, a valid proxy address, or a successful firewall transaction,
the launcher exits without starting Java. Kotlin checks a root-owned non-writable launch record tied to
its PID, boot ID, proxy configuration, socket-policy version, seccomp mode and UID/capabilities before initializing JNI. There is
no confirmation flag and no unrestricted Windows MTProto login; Windows Bot API/development builds work.

Rules apply only to UID 10001, in the shared host namespace, for both IPv4 and IPv6. Reserve this UID for
THB on the host. Run only one THB MTProto container per host. The proxy hostname is resolved before
startup and TDLib receives one pinned IP; restart to refresh DNS or select another proxy address.
Allowed egress: that IP/TCP port, configured DNS resolvers, explicit RFC1918 networks in
`TELEGRAM_EGRESS_LAN_CIDRS`, replies to incoming web connections, and resolved OpenWeather HTTPS
addresses when enabled. All other IP OUTPUT traffic from UID 10001 is rejected. Public remote command destinations
are consequently unavailable in this mode. Do not list a LAN network containing an unrestricted relay.
The seccomp filter applies to the launcher process tree, not arbitrary host processes with UID 10001.
Do not run another process with this UID or pass privileged socket file descriptors into the container.
DNS, weather endpoints, allowed LAN hosts and incoming web connections are explicit trust exceptions;
this is not a sandbox against data tunneling through permitted peers.

Rules replace only `inet thb_mtproto` in one atomic transaction. They remain after shutdown; boot/recreate
always installs them again before Java. Do not flush or replace this table while THB runs. Host root,
Docker administrators and the container image/launcher are trusted; a root administrator can defeat any
host firewall. The launcher is not protection against a compromised host. Test Linux nftables/capability
behavior on the deployment kernel before production use; local test results are reported separately.

### Persistent data permissions (one-time, application stopped)

The main process is non-root in MTProto mode. Existing H2 and TDLib files must be accessible to UID 10001.
After taking a backup and verifying the absolute deployment path, an operator can run on the Pi:

```shell
sudo chown -R 10001:10001 /var/lib/telegram-home-bot
sudo chmod 0700 /var/lib/telegram-home-bot
```

The launcher deliberately does not change ownership recursively. Change the path if THB_DATA_DIR differs.
A TLS keystore must be readable by GID 10001 (for example owner root, group 10001, mode 0640) with
search permission on its parent directory; do not make all secrets world-readable. Remote commands
now run as UID 10001 and cannot elevate privileges. Check their permissions before switching transport.

## Credentials and proxy link

Obtain your own API ID/hash from [Telegram API development tools](https://my.telegram.org/apps)
after signing into my.telegram.org. These are application credentials, separate from the BotFather token.
See [Telegram instructions](https://core.telegram.org/api/obtaining_api_id).
TDLight authenticates the bot with the existing token; it does not ask for a phone login code.

For `tg://proxy?server=proxy.example.invalid&port=443&secret=ee...`, copy the decoded `server`,
`port`, and entire decoded `secret` query values into the corresponding variables. The secret includes
`ee`, the 16-byte key encoded as hex, and the entire hex-encoded TLS domain. Do not strip the domain,
convert to dd, or paste the complete link into TELEGRAM_PROXY_SECRET. The TLS domain can differ from
TELEGRAM_PROXY_HOST. Examples below contain placeholders, not working credentials.

```dotenv
TELEGRAM_ENABLED=true
TELEGRAM_TRANSPORT=mtproto
TELEGRAM_TOKEN=<BotFather-token>
TELEGRAM_BOT_CHAT_ID=<existing-chat-id>
TELEGRAM_HOME_GROUP_USER_IDS=<allowed-user-id>,<another-user-id>
TELEGRAM_API_ID=<positive-api-id>
TELEGRAM_API_HASH=<32-hex-characters>
TELEGRAM_PROXY_HOST=proxy.example.invalid
TELEGRAM_PROXY_PORT=443
TELEGRAM_PROXY_SECRET=<complete-ee-prefixed-hex-secret>
TELEGRAM_TDLIB_DATA_DIR=/app/data/tdlib
TELEGRAM_EGRESS_LAN_CIDRS=192.168.1.0/24
DB_URL=jdbc:h2:/app/data/thb
```

Keep .env permissions 0600. Do not print Compose config without `--quiet`, log native request objects,
or expose session/capture files. There is no environment flag that bypasses the egress guard.

## Access model (both transports)

Authorization uses the immediate sender's user ID. A user is allowed if that positive ID matches
`TELEGRAM_BOT_CHAT_ID` or appears in `TELEGRAM_HOME_GROUP_USER_IDS`, subject to login-attempt
blocking. A negative group chat ID does not identify an owner; list authorized users explicitly
when notifications target a group.

`TELEGRAM_HOME_GROUP_USER_IDS` is a user allowlist. The application does not query actual group
membership or grant access to everyone in a group. There is no allowed-chat restriction:
an authorized user can invoke actions in any chat with the bot. Command replies go to the source
chat and may be visible to its other participants; notifications go to the configured chat.
Anonymous/channel senders do not qualify as allowlisted users. These rules apply to both
text commands and callbacks through the shared application handler.

## Storage and runtime

Inside the existing data mount, `tdlib/database` stores authorization and TDLib state and `tdlib/files`
is the downloads directory. `session.lock` prevents simultaneous access. Windows defaults to
`data/tdlib`; the directory is excluded from Git. Store custom paths outside the checkout. The Docker launcher requires paths within /app/data.
The database is not encrypted by this implementation: protect it with filesystem permissions and
protected backups. Back up only after a clean stop. Do not share a session between bots or machines.
A saved session's bot identity must match the configured token prefix.

ARM64 uses GNU/OpenSSL3 natives. The image includes libssl3, zlib1g and libstdc++6; Windows requires
the Visual C++ x64 runtime. Both ARM64 and Windows classifiers are packaged so Windows builds can
be deployed to Pi. Windows AMD64 JNI loading was smoke-tested without creating a client. The ARM64 ELF dependencies were inspected (OpenSSL 3, GLIBC through 2.34, GLIBCXX through 3.4.29); ARM64 JNI loading also passed under Docker Desktop emulation. Pi memory/CPU and
actual telemt compatibility still require measurement. Text commands older than five minutes (or more than one minute in the future) are ignored in both transports to avoid executing stale offline commands. Callback targets are resolved against the current menu. The bounded command queue holds 100 events;
slow shell/weather work runs outside TDLib callbacks. Queue overflow is rejected, never replayed as
another machine command. Dedupe is bounded and process-local, not crash-safe exactly-once delivery.

Updates are queued independently of connection-ready state and wait for bot identity verification.
Temporary disconnects do not discard already received updates. Outgoing requests wait for identity;
configured notifications additionally wait for `GetChat(TELEGRAM_BOT_CHAT_ID)`. In the pinned native
revision, `GetChatRequest` calls `MessagesManager::load_dialog`, which loads unknown users/groups/channels
for bots ([source](https://github.com/tdlight-team/tdlight/blob/ccf8956e63d29074009867859ecad251c75f36e1/td/telegram/MessagesManager.cpp)).
An inaccessible destination fails startup visibly. TDLib owns retries for submitted sends; the application
does not resend after an ambiguous timeout. At most 100 Java outgoing operations wait at once, each with
a 60-second deadline; native delivery can complete later. This is not a durable notification queue.

Callback receipt is acknowledged on the control executor, independently of slow commands. Authorization
and duplicate-action prevention still happen in the shared handler before any privileged operation.
Commands merge stdout/stderr, drain output continuously, keep at most `COMMAND_MAX_OUTPUT_BYTES`
(default 65536, range 1024–1048576), and time out after `COMMAND_TIMEOUT_SECONDS` (default 30, range 1–300).
Timeout/interruption kills the child and its observed descendants. This is a resource limit, not a process
sandbox: deliberately daemonized children may escape descendant tracking. These limits also apply to Bot API/web actions.

## Build and deployment

Use JDK 17:

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootJar
# After preparing data permissions and testing the Linux guard:
.\gradlew.bat firstDeploy "-PpiHost=<pi>" "-PpiUser=<user>" "-PpiGroup=<group>" "-PdockerPlatform=linux/arm64" "-PsendEnv=true" "-PmtprotoDeployment=true"
```

The Dockerfile/launcher changed, so rebuild the image with firstDeploy; redeploy alone only updates the JAR.
The new JAR refuses the previous launcher's attestation: libseccomp2 and socket-policy version 3 are mandatory.
The SSH deployment user only needs the data directory to exist; Java needs write access as UID 10001.
Do not rerun deployBootstrapRemote after changing ownership without restoring the required runtime ownership.
No deployment is performed by tests. Preserve host networking and the existing H2 data mount.
The MTProto HealthIndicator reports UP only after proxy setup, bot identity, configured chat resolution and
ConnectionStateReady; initialization/disconnection reports OUT_OF_SERVICE and terminal failure reports DOWN.
It still does not prove a Telegram round trip. A temporary outage leaves TDLib reconnecting. A terminal
authorization/callback/control-queue failure exits the JVM for Docker's restart policy, rather than leaving
an inert client running. Close waits at most 20 seconds and factory cleanup 10; session locks are retained
while native cleanup is unfinished. A terminal-failure watchdog halts after 55 seconds; Compose allows 60 seconds to stop.
On a 1 GB Pi start with JAVA_TOOL_OPTIONS=-Xms64m -Xmx256m -XX:ActiveProcessorCount=2,
then measure total RSS: JNI memory is outside the Java heap. This is a starting budget, not a tested capacity guarantee.

Offline Compose validation without loading a real .env:

```powershell
$env:THB_ENV_FILE='.env.example'
docker compose --env-file .env.example --project-directory . -f deploy/docker-compose.yml -f deploy/docker-compose.mtproto.yml config --quiet
Remove-Item Env:THB_ENV_FILE
```

ARM64 image compatibility check (requires Docker Engine/Buildx):

```shell
docker buildx build --platform linux/arm64 -f deploy/Dockerfile --load -t thb-image:mtproto .
```

## Manual verification (test bot first)

1. Verify aarch64 OS, JDK 17, native dependencies, available RAM, and firewall boot ordering.
2. Stop all previous pollers for the test token. Configure credentials locally; no credentials in commands.
3. Verify the installed `inet thb_mtproto` guard with a probe running as UID 10001: proxy endpoint allowed; other Internet
   destinations denied for IPv4 and IPv6. Preserve access to web/LAN/WoL/weather.
4. Capture uplink traffic before starting: `sudo tcpdump -ni <interface> -w <private-path> 'tcp or udp'`.
   Correlate `sudo ss -tpn` with the application's PID/cgroup and firewall counters. Do not capture only
   the proxy address: that would hide leaks. Restrict capture permissions.
5. Start MTProto with both Compose files and verify the guard. Send text, /menu, /features, /wol, /weather,
   reply-keyboard commands and a harmless configured echo command. Check inline edit and callback ACK,
   reply-to-message, notifications, unauthorized text/callback and group anonymous senders.
   On a fresh session, verify the first notification to both a private and a group destination
   without a preceding incoming message (use separate test configurations). Queue several
   notifications during startup and confirm FIFO delivery after destination resolution.
6. Confirm working Telegram round trips while direct egress is denied. All TDLib established connections
   must terminate at telemt. Optionally correlate with counters supplied by the server operator.
7. Block the proxy endpoint locally (do not modify the external server) for 15 minutes. Delivery must
   stop, no direct DC/bootstrap connection may succeed. Native blocked attempts may appear in counters.
8. Restore proxy access; check reconnect and absence of duplicate machine operations. Repeat with a
   saved session, Docker recreate and reboot. Test wrong secret, refused port and DNS failure.
   Separately interrupt proxy access while startup is resolving the notification chat with `GetChat`.
   A transient outage should retry and recover without an unnecessary terminal restart or lost
   buffered notifications. Check request results separately from delivery updates; a callback ACK
   confirms receipt, not successful execution of a command.
9. Stop the container; verify clean TDLib Close and retained session after restart. Run a 24-hour soak
   with network scanning/weather, recording RSS (including native memory), CPU and disk usage.
10. On the test deployment, simulate a partial release failure and exercise the coordinated Bot API
    rollback below. Preserve a consistent JAR checksum, image tag/digest, Compose/environment files
    and stopped H2/TDLib backups. Confirm an actual Telegram round trip after rollback, retained
    data and unchanged unrelated firewall tables/routes, including WireGuard if used.

Record the application commit, image digest, Pi OS/kernel/architecture, tested telemt version/digest,
test configuration without secrets, scenario outcomes and resource measurements. A mutable image
tag alone does not identify the tested release. These scenarios are acceptance work to perform;
their presence in this checklist does not mean they have passed.

## Diagnostics and rollback

A credential/proxy validation error names the environment variable without its value. Native loading
errors require checking architecture/OpenSSL/VC runtime and writable temporary directories. Check
system clock, proxy endpoint, secret suffix and host deny counters for handshake failures. `AddProxy`
success only validates local configuration, not server availability. Do not enable verbose native logs
with real credentials. Sends during disconnection are submitted once after their startup gates complete;
TDLib performs network retries. Timeouts and request types, delivery error codes, and terminal-failure stages
are logged without tokens, proxy secrets, request bodies or peer IDs.

MTProto JAR-only rollback is disabled in `deployUp`, `deployRestart` and `deployRollback`. A failed deployment
returns an error and leaves the failed release for diagnosis. Keep a consistent previous release before upgrading:
JAR, image tag/digest, Compose files and environment. Remote validation checks that TELEGRAM_TRANSPORT agrees
with -PmtprotoDeployment before installing the JAR. Bot API retains its existing JAR rollback, with the same remote check.

For a coordinated rollback to Bot API, stop the MTProto container, restore the backed-up Bot API image/JAR,
set TELEGRAM_TRANSPORT=bot-api in the restored environment, omit the MTProto Compose override and use
-PmtprotoDeployment=false (default), then recreate. Verify a Telegram round trip as well as HTTP health.
Bot API retains its original runtime UID. Keep TDLib data for later recovery; do not delete H2 or log out the bot session.
Unset API/proxy settings if desired. Never switch transport automatically on outage.

## Remaining validation and implementation gaps

- The privileged launcher and native library need an ARM64 kernel/image smoke test before production use.
- ARM64 image build and JNI loading passed in Docker Desktop. Full guarded ARM64 startup is blocked by the emulator: nft cannot initialize a Netlink socket (Protocol not supported). The launcher correctly refuses to start Java. Native Linux AMD64 Docker launcher + Kotlin EgressGuard and socket tests passed. Physical Pi startup and live Telegram/telemt still require verification.
- The reusable client exposes request results and separate TDLib send-success/send-failure updates.
  These do not prove recipient reading. Notification retries and durable command execution journal are not implemented.
- Slash commands must match configured aliases; /command@botname normalization is not implemented.
- Callback ACK is dispatched outside business operations; its delivery still depends on proxy availability.
- Native shutdown/reconnect and full keyboard/feature parity still need live test-bot verification.

Do not describe the release as satisfying all production acceptance criteria until these gates are closed.

## Reproducing automated guard tests

Policy tests (no privileges or network traffic); Linux additionally tests seccomp in a child process across exec:

```shell
python -m unittest discover -s deploy/python -p 'test_*.py'
```

Linux kernel test uses only loopback sockets in a disposable network namespace and refuses the host namespace:

```shell
sudo unshare --net python3 deploy/python/test-egress-linux.py
```

It checks allowed proxy TCP, rejected alternative IPv4/IPv6 TCP, replacement of the nft table, endpoint outage,
and UID/capabilities/no-new-privileges after exec. It also checks packet/raw TCP rejection with NET_RAW present
and raw ICMP socket availability. Requires Python 3, libseccomp2, nftables and iproute2. This is not a live
Telegram handshake test and does not replace a Raspberry Pi ARM64 image smoke test.

Offline deployment-policy regression checks (evaluate command builders only; no SSH/SCP/Docker actions):

```shell
./gradlew -I deploy/test-deployment-policy.init.gradle testDeploymentPolicy -PpiUser=tester -PpiHost=example.invalid -PmtprotoDeployment=true
./gradlew -I deploy/test-deployment-policy.init.gradle testDeploymentPolicy -PpiUser=tester -PpiHost=example.invalid -PmtprotoDeployment=false
# On Linux, execute generated shell commands against temporary files and a fake Docker:
python3 -m unittest discover -s deploy/python -p 'test_*.py'
```

For the complete automated workflow, including fixture generation and platform-specific skips,
see [developer verification](verification.md).

See [verification status and limits](verification.md#recorded-verification-baseline) for the dated
automated baseline. Automated results do not establish live acceptance for the current checkout;
complete the manual checklist above on the target Pi before claiming production readiness.

Deployment setup: [Raspberry Pi guide](../deploy/docs/raspberry-pi-deployment.md) and [ARM64 Docker installation](../deploy/docs/raspberry-pi-3-docker-arm64.md).
