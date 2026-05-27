GRADLE ?= gradlew
GRADLE_ARGS ?=

# Build the jar
build-jar:
	$(GRADLE) bootJar $(GRADLE_ARGS)

# Build Docker image and save to tar
build-image:
	$(GRADLE) deployDockerSaveImage $(GRADLE_ARGS)

# Send jar to Raspberry Pi
send-jar:
	$(GRADLE) deploySendJar $(GRADLE_ARGS)

# Send Docker image to Raspberry Pi (first deploy only)
send-image:
	$(GRADLE) deploySendImage $(GRADLE_ARGS)

# Send docker-compose.yml to Raspberry Pi (first deploy only)
send-config:
	$(GRADLE) deploySendCompose $(GRADLE_ARGS)

# Send .env to Raspberry Pi explicitly (opt-in because it may contain secrets)
send-env:
	$(GRADLE) deploySendEnv -PsendEnv=true $(GRADLE_ARGS)

# Load image and start container on Raspberry Pi (first deploy only)
up:
	$(GRADLE) deployUp $(GRADLE_ARGS)

# Restart container on Raspberry Pi (after jar update)
restart:
	$(GRADLE) deployRestart $(GRADLE_ARGS)

# Full daily update cycle: build jar → send → restart
redeploy:
	$(GRADLE) redeploy $(GRADLE_ARGS)

# One-time first deploy: build image → send everything → start
first-deploy:
	$(GRADLE) firstDeploy $(GRADLE_ARGS)

# View container logs
logs:
	$(GRADLE) deployLogs $(GRADLE_ARGS)

# Stop container
stop:
	$(GRADLE) deployStop $(GRADLE_ARGS)

# Check container status
status:
	$(GRADLE) deployStatus $(GRADLE_ARGS)
