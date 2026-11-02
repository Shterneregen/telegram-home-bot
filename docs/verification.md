# Developer verification

Run commands from the repository root with JDK 17. Examples use `./gradlew`; in PowerShell use
`.\gradlew.bat`. Gradle needs access to dependency repositories on the first run. Add `--offline`
only when dependencies are already cached. No credentials are required for the automated checks below.

## JVM checks and packaging

```shell
./gradlew projects
./gradlew :telegram-tdlight:check
./gradlew verifyTelegramModuleBoundaries
./gradlew test
./gradlew bootJar
```

`projects` should show root and `:telegram-tdlight` only. Library `check` runs tests, ktlint and
boundary verification independently of root application compilation/tests. The explicit boundary
task checks both projects. Root `test` covers application behavior; run it in addition to the
library check. `bootJar` produces `build/libs/thb.jar` with the library and native dependencies.
Compilation and packaging do not prove native loading or network connectivity on Raspberry Pi.

To inspect library task independence without executing tasks:

```shell
./gradlew :telegram-tdlight:check --dry-run
```

For the complete root verification lifecycle:

```shell
./gradlew check
```

The [recorded baseline](#recorded-verification-baseline) includes passing JVM tests, library checks
and packaging, but failing root ktlint checks due to existing formatting violations. Passing
selected checks must not be described as a passing full `check`; inspect its exit status and
reports separately. Test XML is under each project's `build/test-results/test/`; lint reports
are under `build/reports/ktlint/`.

## Independent source consumer

```shell
./gradlew -p samples/tdlight-consumer offlineSmoke --offline
```

Cache dependencies before using offline mode. The sample builds only the library source project
with separate outputs and checks absence of Spring/Bot API dependencies. Its verifier deliberately
rejects startup before JNI. This exercises failure/state, explicit-send rejection and idempotent
close without credentials or network access. It does not verify a published Maven artifact,
successful native startup or a Telegram round trip. See the [sample README](../samples/tdlight-consumer/README.md).

## Deployment policy and launcher tests

Generate both deployment command fixtures before Python discovery:

```shell
./gradlew -I deploy/test-deployment-policy.init.gradle testDeploymentPolicy -PpiUser=tester -PpiHost=example.invalid -PmtprotoDeployment=true
./gradlew -I deploy/test-deployment-policy.init.gradle testDeploymentPolicy -PpiUser=tester -PpiHost=example.invalid -PmtprotoDeployment=false
python -m unittest discover -s deploy/python -p 'test_*.py'
```

The Gradle tasks evaluate command builders and write `build/deployment-policy-{true,false}.json`;
they do not perform SSH/SCP or deployment. Run them again after `clean`, which deletes fixtures.
Linux Python cases execute generated commands in temporary directories against a fake Docker
and check seccomp inheritance across exec in child processes. Windows skips Linux-only cases;
missing fixtures also cause deployment-command cases to skip. Inspect skips before claiming
complete coverage. On Linux use `python3` if `python` is unavailable.

For the privileged kernel test in a disposable Linux network namespace, see
[automated guard tests](telegram-mtproto.md#reproducing-automated-guard-tests).
That test has separate OS/package requirements and is not part of ordinary Python discovery.

## Compose and ARM64 packaging

Validate Compose using placeholders instead of the local credential file (PowerShell):

```powershell
$env:THB_ENV_FILE = '.env.example'
try {
    docker compose --env-file .env.example --project-directory . -f deploy/docker-compose.yml config --quiet
    docker compose --env-file .env.example --project-directory . -f deploy/docker-compose.yml -f deploy/docker-compose.mtproto.yml config --quiet
} finally {
    Remove-Item Env:THB_ENV_FILE
}
```

Requires Docker Compose v2. Each command must exit successfully. Keep `--quiet` to avoid printing
resolved configuration. The commands parse configuration without starting containers.

With Docker Engine and Buildx available:

```shell
docker buildx build --platform linux/arm64 -f deploy/Dockerfile --load -t thb-image:verification .
docker image inspect thb-image:verification --format '{{.Os}}/{{.Architecture}}'
```

Expected architecture: `linux/arm64`. The Dockerfile builds a runtime image; Compose mounts the
application JAR separately. Image construction/inspection does not start Java, load JNI or prove
the privileged launcher works on the deployment kernel.

## Live acceptance

Use the [manual acceptance checklist](telegram-mtproto.md#manual-verification-test-bot-first)
for physical Pi startup, FakeTLS round trips, outage recovery, rollback and resource measurements.
These require a controlled environment and are separate from offline checks.

## Recorded verification baseline

The following results were recorded on 2026-11-01 during consolidation into the single
`telegram-tdlight` library, starting from commit `00a429512fb25bb9268738d89bbb06174ebae72c`.
The environment was Windows with JDK 17.0.9 and Gradle 8.14, with Linux Python tests under
WSL Ubuntu 20.04 as UID 1000. This summary preserves the recorded evidence; it is not a new
test run or a claim about the current checkout.

| Check | Recorded result |
|---|---|
| Library JVM tests | 45 passed, no failures/errors/skips |
| Root JVM tests | 46 passed, no failures/errors/skips |
| Library `check` | Passed, including main/test lint and boundary verification |
| Library independence | Standalone check and dry-run passed without root task dependencies |
| Module boundaries | Passed for the two-project graph and SDK/package isolation |
| `bootJar` | Passed; one own library JAR, with API, Core and Linux Guard, inside `thb.jar` |
| Independent consumer | Passed offline, without JNI or network access |
| Root ktlint | Failed: 393 main and 22 test violations in the existing baseline |
| Python discovery on Windows | 7 passed, 6 Linux-only skipped |
| Python discovery on WSL | 13 passed, no skips, after generating both deployment fixtures |
| Deployment policy | Passed for both transport modes; command builders only |
| Compose configuration | Base and MTProto override passed with `.env.example` |
| ARM64 runtime image | Built and inspected as `linux/arm64`; no application startup |

Packaging inspection found both Windows AMD64 and Linux ARM64 native artifacts. The ARM64
library had an AArch64 ELF header. Neither that inspection nor the image build proves successful
guarded startup on a physical Pi. Full root `clean check` was not established as passing;
root lint was checked separately and failed.

Physical Pi startup, actual Telegram/telemt handshakes and round trips, outage/reconnect,
24-hour soak, total native/JVM memory usage and coordinated rollback were not verified in
this baseline. No production deployment, artifact publication or privileged host-namespace
firewall test was performed. Source-project reuse does not establish Maven publication independence.
