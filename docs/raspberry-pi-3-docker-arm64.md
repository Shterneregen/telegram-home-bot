# Install Docker and Docker Compose on Raspberry Pi 3 (ARM64)

This guide installs **Docker Engine**, **Docker Compose v2**, and **Docker Buildx** on a **Raspberry Pi 3** running a 64-bit Raspberry Pi OS / Debian-based system (`arm64` / `aarch64`).

> Recommended approach: use Docker's official APT repository instead of the convenience `curl | sh` installer.

## 1. Verify the system architecture

Run:

```bash
uname -m
dpkg --print-architecture
cat /etc/os-release
```

Expected architecture values:

```text
aarch64
arm64
```

If you see `armhf` or `armv7l`, you are running a 32-bit OS and this ARM64 guide does not apply directly.

## 2. Remove old or conflicting Docker packages

This step is safe even if Docker has not been installed before.

```bash
sudo apt remove -y \
  docker.io \
  docker-compose \
  docker-doc \
  docker-buildx \
  podman-docker \
  containerd \
  runc
```

## 3. Add Docker's official APT repository

Update package lists and install the required tools:

```bash
sudo apt update
sudo apt install -y ca-certificates curl
```

Create the keyring directory:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
```

Download Docker's GPG key:

```bash
sudo curl -fsSL https://download.docker.com/linux/debian/gpg \
  -o /etc/apt/keyrings/docker.asc
```

Make the key readable by APT:

```bash
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

Add the Docker repository:

```bash
sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/debian
Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
```

Refresh package lists:

```bash
sudo apt update
```

## 4. Install Docker Engine and Docker Compose

Install Docker Engine, the CLI, containerd, Buildx, and Docker Compose v2:

```bash
sudo apt install -y \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin
```

## 5. Enable and start Docker

```bash
sudo systemctl enable --now docker
```

Check the service status:

```bash
sudo systemctl status docker
```

Press `q` to exit the status view.

## 6. Run Docker without `sudo`

Add your current user to the `docker` group:

```bash
sudo usermod -aG docker $USER
```

Apply the new group membership immediately:

```bash
newgrp docker
```

Alternatively, log out and log back in.

> Security note: membership in the `docker` group effectively grants root-level access to the system. Only add trusted users.

## 7. Verify the installation

Run Docker's test container:

```bash
docker run --rm hello-world
```

Check installed versions:

```bash
docker version
docker compose version
docker buildx version
```

Modern Docker Compose uses:

```bash
docker compose
```

instead of the legacy command:

```bash
docker-compose
```

## 8. Test Docker Compose

Create a test directory:

```bash
mkdir -p ~/docker-test
cd ~/docker-test
```

Create `compose.yaml`:

```bash
cat > compose.yaml <<'EOF'
services:
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    restart: unless-stopped
EOF
```

Start the container:

```bash
docker compose up -d
```

Check its status:

```bash
docker compose ps
```

Open the following address from another device on the same network:

```text
http://RASPBERRY_PI_IP:8080
```

Stop and remove the test container:

```bash
docker compose down
```

## Optional: Limit Docker log growth

On a Raspberry Pi, especially when using a microSD card, it is a good idea to prevent container logs from growing indefinitely.

Create or edit Docker's daemon configuration:

```bash
sudo mkdir -p /etc/docker
sudo nano /etc/docker/daemon.json
```

Use the following configuration:

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

Restart Docker:

```bash
sudo systemctl restart docker
```

Verify Docker is still working:

```bash
docker info
```

## Useful commands

Show running containers:

```bash
docker ps
```

Show all containers:

```bash
docker ps -a
```

Show Docker disk usage:

```bash
docker system df
```

Show Compose services:

```bash
docker compose ps
```

View Compose logs:

```bash
docker compose logs -f
```

Stop a Compose project:

```bash
docker compose down
```

## Official documentation

- Docker Engine on Debian: https://docs.docker.com/engine/install/debian/
- Linux post-installation steps: https://docs.docker.com/engine/install/linux-postinstall/
- Docker Compose: https://docs.docker.com/compose/
