# Telegram Home Bot

Can be used for home automation on Raspberry Pi

## What can this bot do?

- Monitor local network and notify about hosts appearances/disappearances via Telegram client
- Run commands on target machine from Telegram client
- Wake-up selected hosts via WOL technology sending magic packet using Telegram client
- Provide weather info for chosen places using OpenWeatherMap API via Telegram client

Application settings are stored in [application.properties](src/main/resources/application.properties) file

## How to set up the project

### Create telegram chatbot

- Start a chat with **@BotFather**
- Use message **/newbot**
- Set unique bot name (ends with `_bot` or `Bot`, use it as **telegram.bot-name**)
- Get **token** from the final message (and use it as **telegram.token**)
- Start conversation with bot
- Retrieve **chat_id**, call https://api.telegram.org/bot[YOUR_TOKEN]/getUpdates
  (and use it as **TELEGRAM_BOT_CHAT_ID** env variable)

### Admin site in browser

- Located by default on http://127.0.0.1:9988/
    - Port setting: `server.port`
- Default users: `admin / 1234`, `user / 1234`
    - Default credentials could be changed in settings:
      `default.admin.login / default.admin.password`, `default.user.login / default.user.password`
- Password could be changed on http://127.0.0.1:9988/updatePassword page

### Enable network monitor

- Set `NETWORK_MONITOR_ENABLED` env var to `true`
- You can observe/add/edit hosts on http://127.0.0.1:9988/hosts page
- Hosts availability changes is on http://127.0.0.1:9988/hosts/time-log page
- Unfortunately, current implementation is based on `ip -j n show` and some other native calls, so you have to
  install `iproute2` and `fping`on Linux machine where chatbot is running. See [Dockerfile](Dockerfile)
- This feature does not work on Windows
- To enable/disable host notifications, run the `/features` command in the Telegram client and choose what you want

#### What does network monitor do?

- Periodically calls a command (**STATE_CHANGE_COMMAND**) to check network changes
- Chatbot notifies about hosts appearances/disappearances **TELEGRAM_BOT_CHAT_ID** user

### Run commands on chatbot machine

- You can add any initial commands that will be available to run
  in [commands.properties](src/main/resources/commands.properties) file
- Besides, you can add/edit commands on http://127.0.0.1:9988/commands page
- To hide any command from the Telegram client, uncheck the `Enabled` checkbox on the Command editing page
- In the Telegram client these commands will be available as a reply markup

### Wake On Lan feature

- Modify `WAKE_ON_LAN_BROADCAST_IP` env var according to your network mask
- To enable WOL for a host open http://127.0.0.1:9988/hosts page
- Choose a host and check `Wake On Lan Enabled` checkbox
- To wake up a host, run `/wol` in the Telegram client and select the one you want

### Enable Weather menu

- Set `OPENWEATHER_ENABLED` env variable to `true`
- Generate you API key on https://home.openweathermap.org/api_keys page
- Set the API key above as `OPENWEATHER_APPID` env variable
- Add places where you want to know the weather on http://127.0.0.1:9988/weather page
    - Don't add `city ID` with `lat & lon` in the same time, use them separately
    - City ID could be found in [city.list.json.gz](http://bulk.openweathermap.org/sample/city.list.json.gz)
      or [city.list.min.json.gz](http://bulk.openweathermap.org/sample/city.list.min.json.gz)
- After starting the app you can reach the Weather menu using `/weather` command via Telegram client
- Please note that the free subscription includes: 60 calls/minute, 1 000 000 calls/month
  ([Pricing](https://openweathermap.org/price))
- Useful links
    - [Weather API](https://openweathermap.org/api)
    - [Current weather data](https://openweathermap.org/current)
    - [Index of /sample/](http://bulk.openweathermap.org/sample/)

### List of chatbot commands

- `/menu` - Main menu
- `/features` - Features Setting (notification settings for now)
- `/wol` - Wake On Lan hosts
- `/weather` - Weather

---

## Additional information

### [Installation as an init.d Service](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#deployment-initd-service)

- Run `gradlew clean bootJar` in the project root folder
- Executable jar will be placed here: `build/libs/thb.jar`
- Copy the jar file to SOME_LINUX_FOLDER on Linux machine

- Run on Linux machine:

```shell
sudo mkdir /var/telegram # create a folder for jar file
sudo cp /SOME_LINUX_FOLDER/thb.jar /var/telegram/thb.jar # copy jar to the folder
sudo ln -s /var/telegram/thb.jar /etc/init.d/thb # create symlink the jar to init.d
sudo chmod +x /var/telegram/thb.jar # make thb.jar executable
sudo chown -R $USER:$USER /var/telegram
sudo chmod 755 /var/telegram
sudo systemctl daemon-reload # reload systemd manager configuration
sudo service thb start # start bot as a service
update-rc.d thb defaults # autostart
```

- When I'm using THB as a Linux service, I just put the `.env` file (with all necessary props) next to the jar file

### Enable HTTPS

The example below will be with a self-signed certificate on the local machine

- Create `thb-keystore.p12`

```shell
keytool -genkeypair -alias thb -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore thb-keystore.p12 -validity 3650 -ext san=ip:127.0.0.1
```

- You will be asked to enter a password for the keystore
- Set your password as *SSL_KEY_STORE_PASSWORD** property and change other props if necessary
- Add `thb-keystore.p12` to the `Trusted Root Certification Authorities certificate` store (Windows)

### Launch bot in Docker (docker-compose)

#### Prerequisites

- JDK 17 used to run Gradle deployment tasks
- Docker with the Compose plugin installed on the target machine
- SSH access to Raspberry Pi (or any Linux host)
- Passwordless `sudo` for the deployment user when creating and updating system directories
- `.env` created from [`.env.example`](.env.example)

#### Environment and persistent files

```env
DB_URL=jdbc:h2:/app/data/thb
SERVER_PORT=9988
SSL_ENABLED=false
```

Deployment files follow the Linux filesystem hierarchy:

```text
/opt/telegram-home-bot/              # docker-compose.yml and application jar
/etc/telegram-home-bot/              # thb.env and TLS secrets
/var/lib/telegram-home-bot/          # persistent H2 database
/var/backups/telegram-home-bot/      # previous jar used for rollback
/var/cache/telegram-home-bot/        # temporary Docker image archive
```

Do not point `DB_URL` outside `/app/data`; that path is mounted from `/var/lib/telegram-home-bot`.

When Spring Boot terminates TLS itself, place the keystore on the target host and configure:

```env
SSL_ENABLED=true
SSL_KEY_STORE=file:/app/secrets/thb-keystore.p12
SSL_KEY_STORE_PASSWORD=change-me
```

Place the corresponding file at `/etc/telegram-home-bot/secrets/thb-keystore.p12` on the target host.

If HTTPS is terminated by a reverse proxy, keep `SSL_ENABLED=false`.

#### First deploy (one-time setup)

```bash
# /etc/telegram-home-bot/thb.env must already exist
gradlew firstDeploy

# Or install the local .env under /etc with mode 0640
gradlew firstDeploy -PsendEnv=true

# Cross-build for an ARM64 Raspberry Pi
gradlew firstDeploy -PdockerPlatform=linux/arm64
```

This will:
1. Validate `.env`, Compose configuration, and the Gradle JVM version
2. Build a Docker image with JRE 17, curl, and the network tools
3. Create the required `/opt`, `/etc`, and `/var` directories with restricted permissions
4. Save and upload the image through `/var/cache/telegram-home-bot/thb-image.tar`
5. Build and upload the jar as `/opt/telegram-home-bot/thb.jar.new`
6. Preserve the current jar as `/var/backups/telegram-home-bot/thb.jar.bak`
7. Atomically install the new jar, load the image, and remove the uploaded tar archive
8. Recreate the container and wait until the Actuator health endpoint reports `UP`
9. Restore the backup automatically if the new container is unhealthy

> **Note:** `.env` is not sent automatically. Create `/etc/telegram-home-bot/thb.env` manually, or use `gradlew deploySendEnv -PsendEnv=true`.

For an existing installation, stop the old container and copy its database and keystore before the first
FHS-layout deployment. Verify the actual source filenames before copying:

```bash
docker stop thb
sudo find /var/telegram -maxdepth 1 -type f \( -name '*.mv.db' -o -name '*.p12' \) -ls
sudo install -m 0640 -o master -g master /var/telegram/thb-new.mv.db /var/lib/telegram-home-bot/thb.mv.db
sudo install -m 0640 -o root -g master /var/telegram/thb-keystore.p12 /etc/telegram-home-bot/secrets/
```

Do not copy a live H2 database. Keep the old files until the new container is healthy and its data has been verified.

#### Daily update (new code → deploy)

```bash
# Build jar, install it atomically, recreate the container, and verify health
gradlew redeploy
```

#### Gradle deployment tasks

All deployment logic is in [`gradle/deploy.gradle`](gradle/deploy.gradle). Run `gradlew tasks --group deployment` to list them.

| Command | Description |
|---------|-------------|
| `gradlew bootJar` | Build `thb.jar` locally |
| `gradlew deployValidateEnv` | Validate required values and persistent paths in `.env` |
| `gradlew deployValidateCompose` | Run `docker compose config --quiet` |
| `gradlew deployDockerBuildImage` | Build Docker image (add `-PdockerPlatform=linux/arm64` for ARM) |
| `gradlew deployDockerSaveImage` | Save Docker image as `build/deploy/thb-image.tar` |
| `gradlew deployBootstrapRemote` | Create the FHS directories and permissions using `sudo -n` |
| `gradlew deployPrepareRemote` | Verify that the remote directories are writable |
| `gradlew deploySendJar` | Upload the jar as `thb.jar.new` |
| `gradlew deployInstallJar` | Back up the jar under `/var/backups` and atomically install the upload |
| `gradlew deploySendImage` | SCP Docker image tar to Raspberry Pi |
| `gradlew deploySendCompose` | SCP `docker-compose.yml` to Raspberry Pi |
| `gradlew deploySendEnv -PsendEnv=true` | Upload `.env` for installation under `/etc` (opt-in) |
| `gradlew deployInstallEnv -PsendEnv=true` | Install `thb.env` as `root:<deploy-group>` with mode `0640` |
| `gradlew deployValidateRemote` | Validate the remote `.env`, database path, keystore, and Compose file |
| `gradlew deployUp` | Load image, recreate container, and verify health |
| `gradlew deployRestart` | Recreate container and automatically rollback on failed healthcheck |
| `gradlew deployRollback` | Restore `thb.jar.bak`, recreate container, and verify health |
| `gradlew redeploy` | Atomic jar update, container recreation, health verification, and rollback |
| `gradlew firstDeploy` | Build and send all files, then start and verify the container |
| `gradlew deployLogs` | Stream container logs from Raspberry Pi |
| `gradlew deployStop` | Stop container on Raspberry Pi |
| `gradlew deployStatus` | Check container status on Raspberry Pi |

#### Overridable properties

Customize deployment via `-P` flags (defaults shown):

| Property | Default | Description |
|----------|---------|-------------|
| `-PpiHost=192.168.1.15` | `192.168.1.15` | Raspberry Pi IP / hostname |
| `-PpiUser=master` | `master` | SSH user on the Pi |
| `-PpiGroup=master` | `master` | Group allowed to read configuration and update deployment files |
| `-PpiDir=/opt/telegram-home-bot` | `/opt/telegram-home-bot` | Compose and jar directory |
| `-PconfigDir=/etc/telegram-home-bot` | `/etc/telegram-home-bot` | Environment and secrets directory |
| `-PdataDir=/var/lib/telegram-home-bot` | `/var/lib/telegram-home-bot` | Persistent application data |
| `-PbackupDir=/var/backups/telegram-home-bot` | `/var/backups/telegram-home-bot` | Rollback artifacts |
| `-PcacheDir=/var/cache/telegram-home-bot` | `/var/cache/telegram-home-bot` | Temporary transferred image |
| `-PimageName=thb-image:latest` | `thb-image:latest` | Docker image tag to build, save, and load |
| `-PdockerPlatform=` | _(empty)_ | Set to `linux/arm64` for ARM cross-compile |
| `-PdockerComposeCommand="docker compose"` | `docker compose` | Docker Compose command |
| `-PsendEnv=true` | _(unset)_ | Allow `deploySendEnv` to copy local `.env` to the Pi |
| `-PhealthWaitSeconds=120` | `120` | Maximum time to wait for a healthy container |

Example with custom host and user:

```bash
gradlew redeploy -PpiHost=192.168.1.100 -PpiUser=pi
gradlew deployStatus -PpiHost=192.168.1.100 -PpiUser=pi
```

#### Makefile compatibility wrapper

A thin [`Makefile`](Makefile) is kept for convenience, delegating to Gradle:

```bash
make redeploy      # → gradlew redeploy
make first-deploy  # → gradlew firstDeploy
make logs          # → gradlew deployLogs
make stop          # → gradlew deployStop
make status        # → gradlew deployStatus
```

Use `gradlew` / `gradlew.bat` directly for full control, especially `-P` property overrides.

#### Architecture

- **Jar is mounted as a volume**, not baked into the image; updates atomically replace it and recreate the container
- **`network_mode: host`** — required for ARP scanning and Wake-on-LAN
- **Configuration and secrets** live under `/etc/telegram-home-bot` and are mounted read-only
- **Database** persists under `/var/lib/telegram-home-bot`, mounted to `/app/data`
- **Healthcheck** calls `/actuator/health` over HTTP or HTTPS and requires status `UP`
- **Rollback** uses `/var/backups/telegram-home-bot/thb.jar.bak`
- **Image tar** is deleted from `/var/cache` immediately after a successful `docker load`

### Launch SonarQube in Docker

- Run SonarQube container

```shell
docker run -d --name sonarqube -p 9000:9000 -p 9092:9092 sonarqube
```

- Browse http://127.0.0.1:9000. Initial credentials: `admin / admin`
- Change password
- Create project with name `thb`
- Create project token
- Run `sonarqube` task

```shell
gradlew sonarqube -Dsonar.projectKey=thb -Dsonar.host.url=http://127.0.0.1:9000 -Dsonar.login=PROJECT_TOKEN
```

### Create private chat group with chatbot (JFYI)

- open a session with **@BotFather**
- enter **/setjoingroups**
- enter the name of the bot
- enter Disable
- get private group **chat_id**, call https://api.telegram.org/bot$TOKEN/getUpdates

---

## Preview (outdated)

![Commands](./screenshots/Commands.png)

![Hosts](./screenshots/Hosts.png)

![TimeLog](./screenshots/TimeLog.png)

---

Kotlin, Spring (Boot/Security/WebFlux), Gradle, H2, Thymeleaf, Google Charts, telegrambots-springboot-longpolling-starter
