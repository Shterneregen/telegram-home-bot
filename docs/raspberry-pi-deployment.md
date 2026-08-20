# Deploying Telegram Home Bot to Raspberry Pi

This project includes a Docker deployment workflow for Raspberry Pi using Docker Compose and the Gradle tasks from `gradle/deploy.gradle`.

The recommended setup is Raspberry Pi OS 64-bit with a Docker image built for `linux/arm64`.

Set the deployment-specific values once in PowerShell and reuse them in all commands below:

```powershell
$piHost = "<raspberry-pi-host-or-ip>"
$piUser = "<deployment-user>"
$piGroup = "<deployment-group>"
$dockerPlatform = "<docker-platform>"
```

Replace the placeholders with values for your environment before running the commands. For the recommended setup, use `linux/arm64` for `$dockerPlatform`. A hostname such as `raspberrypi.local` can be used instead of a fixed IP if local name resolution is configured. The application default port is `9988`.

Example values for a typical home network:

```powershell
$piHost = "raspberrypi.local" # or, for example, "192.168.1.50"
$piUser = "deploy"
$piGroup = "deploy"
$dockerPlatform = "linux/arm64"
```

The example values are illustrative. Use the actual hostname or IP, Unix user/group, and Docker platform from your environment.

## 1. Prepare the Raspberry Pi

Install Raspberry Pi OS 64-bit and enable SSH. After the first login, update the system:

```bash
sudo apt update
sudo apt full-upgrade -y
sudo apt install -y openssh-server curl ca-certificates
sudo systemctl enable --now ssh
```

Find the Raspberry Pi IP address:

```bash
hostname -I
```

Install Docker Engine and the Docker Compose plugin using the official Docker instructions. Then add the deployment user to the Docker group:

```bash
sudo usermod -aG docker <deployment-user>
```

Log out and back into SSH, then verify the installation:

```bash
docker --version
docker compose version
docker run hello-world
```

The deployment user must have SSH access and permission to run Docker.

## 2. Configure SSH

On the computer used for deployment, create an SSH key and install it on the Pi:

```powershell
ssh-keygen -t ed25519
ssh-copy-id "${piUser}@${piHost}"
```

If `ssh-copy-id` is not available in PowerShell:

```powershell
Get-Content $env:USERPROFILE\.ssh\id_ed25519.pub | ssh "${piUser}@${piHost}" "mkdir -p ~/.ssh; cat >> ~/.ssh/authorized_keys"
```

Verify passwordless login:

```powershell
ssh "${piUser}@${piHost}"
```

The one-time `deployBootstrapRemote` task uses interactive `sudo` to create deployment directories. Routine deployment tasks do not require `sudo`, and no passwordless sudoers rule is needed. Membership in the Docker group grants root-equivalent access; use a dedicated deployment account and protect its SSH key.

## 3. Prepare `.env`

Create a local `.env` file from `.env.example` and configure Docker-specific values:

```env
DB_URL=jdbc:h2:/app/data/thb
DB_USERNAME=sa
DB_PASSWORD=

SERVER_PORT=<application-port>

TELEGRAM_ENABLED=false
TELEGRAM_TOKEN=
TELEGRAM_BOT_CHAT_ID=
TELEGRAM_HOME_GROUP_USER_IDS=

OPENWEATHER_ENABLED=false
OPENWEATHER_APPID=

NETWORK_MONITOR_ENABLED=true
NETWORK_MONITOR_AUTO_DETECT=true
NETWORK_MONITOR_INTERFACE=
NETWORK_MONITOR_LAN_CIDR=
NETWORK_MONITOR_MAX_SCAN_ADDRESSES=1024
BROADCAST_PING_COMMAND_LINUX=

WAKE_ON_LAN_BROADCAST_IP=

SSL_ENABLED=false
```

Do not use a Windows path in `DB_URL` when running in Docker. The path must point inside `/app/data`, which is mounted to the persistent Raspberry Pi directory `/var/lib/telegram-home-bot`.

Do not commit `.env` to Git or publish its tokens. If a Telegram token has already been exposed, revoke and regenerate it through BotFather.

With auto-detection enabled, the application uses the private IPv4 interface associated with the default route and calculates both the scan CIDR and Wake-on-LAN broadcast address. The container must keep `network_mode: host`. To select a specific interface, set, for example, `NETWORK_MONITOR_INTERFACE=eth0`. Explicit `NETWORK_MONITOR_LAN_CIDR` and `WAKE_ON_LAN_BROADCAST_IP` values take priority over detection.

Auto-detection stops with a clear error when interface selection is ambiguous. It excludes loopback, Docker/veth, common VPN interfaces, public networks, and networks containing more addresses than `NETWORK_MONITOR_MAX_SCAN_ADDRESSES`. A trusted custom Linux shell command can be supplied through `BROADCAST_PING_COMMAND_LINUX`.

For the initial deployment, it is recommended to keep Telegram, OpenWeather, and TLS disabled. After verifying the web interface, enable the required integrations:

```env
TELEGRAM_ENABLED=true
TELEGRAM_TOKEN=<new-token>
TELEGRAM_BOT_CHAT_ID=<chat-id>
TELEGRAM_HOME_GROUP_USER_IDS=<id1,id2>

OPENWEATHER_ENABLED=true
OPENWEATHER_APPID=<api-key>
```

## 4. Requirements on the development computer

The local computer must have:

- JDK 17;
- Docker Desktop with Buildx support;
- SSH and SCP;
- a Git checkout of the project.

The deployment tasks require Gradle to run on JDK 17.

Verify the tools:

```powershell
java -version
docker version
docker buildx version
```

## 5. First deployment

Create the remote directories once. This task opens an interactive SSH session so `sudo` can request the Raspberry Pi password:

```powershell
.\gradlew.bat deployBootstrapRemote `
  "-PpiHost=$piHost" `
  "-PpiUser=$piUser" `
  "-PpiGroup=$piGroup"
```

Then run the first deployment:

```powershell
.\gradlew.bat firstDeploy `
  "-PpiHost=$piHost" `
  "-PpiUser=$piUser" `
  "-PpiGroup=$piGroup" `
  "-PdockerPlatform=$dockerPlatform" `
  "-PsendEnv=true"
```

The `-PpiHost`, `-PpiUser`, and `-PpiGroup` values are read from the variables defined at the beginning of this document.

The command:

1. validates the `.env` and Compose configuration;
2. builds the JAR;
3. builds the ARM64 Docker image;
4. uploads the JAR, image, and `docker-compose.yml`;
5. installs `.env` as `/etc/telegram-home-bot/thb.env` with mode `0600`;
6. starts the container;
7. waits for a successful healthcheck.

If `.env` has already been created manually on the Pi at `/etc/telegram-home-bot/thb.env`, run `firstDeploy` without `-PsendEnv=true`.

## 6. Verify the deployment

Check the container status:

```powershell
.\gradlew.bat deployStatus "-PpiHost=$piHost" "-PpiUser=$piUser"
```

View the logs:

```powershell
.\gradlew.bat deployLogs "-PpiHost=$piHost" "-PpiUser=$piUser"
```

The web interface is available at:

```text
http://${piHost}:<server-port-from-thb.env>
```

After the first login, change the initial password using `/updatePassword`. The initial credentials are defined in `src/main/resources/application.yaml`.

## 7. Update the application

After changing the code, run:

```powershell
.\gradlew.bat redeploy `
  "-PpiHost=$piHost" `
  "-PpiUser=$piUser"
```

This task uploads the current Compose file, installs the new JAR, restarts the container, checks the health endpoint, and uses the previous JAR as a rollback if deployment fails. It does not rebuild the Docker image. After changing `Dockerfile` or system packages, run `firstDeploy` again with `dockerPlatform`.

Stop the application:

```powershell
.\gradlew.bat deployStop "-PpiHost=$piHost" "-PpiUser=$piUser"
```

## 8. Configuration notes

- The H2 database is stored on the Pi in `/var/lib/telegram-home-bot`.
- The container uses `network_mode: host`, so the application is available directly through the Raspberry Pi IP address.
- Automatic LAN detection is suitable for a Pi with one private LAN and `network_mode: host`. For multiple interfaces, VPNs, or VLANs, set `NETWORK_MONITOR_INTERFACE` or explicit CIDR/broadcast values.
- Wi-Fi client isolation, VLAN rules, and routed networks may block Wake-on-LAN broadcast traffic even when the broadcast address is correct.
- TLS can be enabled later. With TLS enabled, the keystore must be placed on the Pi in `/etc/telegram-home-bot/secrets/`, and `SSL_KEY_STORE` must be set to `file:/app/secrets/thb-keystore.p12`.
- The `linux/arm64` image will not run on a 32-bit Raspberry Pi OS. A 64-bit OS is recommended; `linux/arm/v7` should be tested separately.

## 9. Deployment directories on the Pi

```text
/opt/telegram-home-bot/              # JAR and docker-compose.yml
/etc/telegram-home-bot/              # thb.env and secrets/
/var/lib/telegram-home-bot/          # persistent H2 database
/var/backups/telegram-home-bot/      # backup JAR
/var/cache/telegram-home-bot/        # temporary Docker image
```
