PROJECT_ROOT := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)
ifeq ($(OS),Windows_NT)
GRADLE ?= "$(PROJECT_ROOT)/gradlew.bat"
else
GRADLE ?= "$(PROJECT_ROOT)/gradlew"
endif
GRADLE_ARGS ?=

# Build the jar
build-jar:
	$(GRADLE) -p "$(PROJECT_ROOT)" bootJar $(GRADLE_ARGS)

# Build Docker image and save to tar
build-image:
	$(GRADLE) -p "$(PROJECT_ROOT)" deployDockerSaveImage $(GRADLE_ARGS)

# Send jar to Raspberry Pi
send-jar:
	$(GRADLE) -p "$(PROJECT_ROOT)" deploySendJar $(GRADLE_ARGS)

# Send Docker image to Raspberry Pi (first deploy only)
send-image:
	$(GRADLE) -p "$(PROJECT_ROOT)" deploySendImage $(GRADLE_ARGS)

# Send docker-compose.yml to Raspberry Pi (first deploy only)
send-config:
	$(GRADLE) -p "$(PROJECT_ROOT)" deploySendCompose $(GRADLE_ARGS)

# Send .env to Raspberry Pi explicitly (opt-in because it may contain secrets)
send-env:
	$(GRADLE) -p "$(PROJECT_ROOT)" deploySendEnv -PsendEnv=true $(GRADLE_ARGS)

# Load image and start container on Raspberry Pi (first deploy only)
up:
	$(GRADLE) -p "$(PROJECT_ROOT)" deployUp $(GRADLE_ARGS)

# Restart container on Raspberry Pi (after jar update)
restart:
	$(GRADLE) -p "$(PROJECT_ROOT)" deployRestart $(GRADLE_ARGS)

# Full daily update cycle: build jar → send → restart
redeploy:
	$(GRADLE) -p "$(PROJECT_ROOT)" redeploy $(GRADLE_ARGS)

# One-time first deploy: build image → send everything → start
first-deploy:
	$(GRADLE) -p "$(PROJECT_ROOT)" firstDeploy $(GRADLE_ARGS)

# View container logs
logs:
	$(GRADLE) -p "$(PROJECT_ROOT)" deployLogs $(GRADLE_ARGS)

# Stop container
stop:
	$(GRADLE) -p "$(PROJECT_ROOT)" deployStop $(GRADLE_ARGS)

# Check container status
status:
	$(GRADLE) -p "$(PROJECT_ROOT)" deployStatus $(GRADLE_ARGS)
