APP_NAME = thb
IMAGE_NAME = thb-image:latest

PI_USER = master
PI_HOST = 192.168.1.15
PI_DIR = /var/telegram

# Build the jar
build-jar:
	gradlew clean bootJar

# Build Docker image and save to tar
build-image:
	docker build -t $(IMAGE_NAME) .
	docker save $(IMAGE_NAME) -o thb-image.tar

# Send jar to Raspberry Pi
send-jar:
	scp build/libs/thb.jar $(PI_USER)@$(PI_HOST):$(PI_DIR)/

# Send Docker image to Raspberry Pi (first deploy only)
send-image:
	scp thb-image.tar $(PI_USER)@$(PI_HOST):$(PI_DIR)/

# Send config files to Raspberry Pi (first deploy only)
send-config:
	scp docker-compose.yml .env $(PI_USER)@$(PI_HOST):$(PI_DIR)/

# Load image and start container on Raspberry Pi (first deploy only)
up:
	ssh $(PI_USER)@$(PI_HOST) \
		"cd $(PI_DIR) \
		&& docker load -i thb-image.tar \
		&& docker-compose up -d"

# Restart container on Raspberry Pi (after jar update)
restart:
	ssh $(PI_USER)@$(PI_HOST) "cd $(PI_DIR) && docker-compose restart"

# Full daily update cycle: build jar → send → restart
redeploy: build-jar send-jar restart

# One-time first deploy: build image → send everything → start
first-deploy: build-image send-image build-jar send-jar send-config up

# View container logs
logs:
	ssh $(PI_USER)@$(PI_HOST) "cd $(PI_DIR) && docker-compose logs -f"

# Stop container
stop:
	ssh $(PI_USER)@$(PI_HOST) "cd $(PI_DIR) && docker-compose down"

# Check container status
status:
	ssh $(PI_USER)@$(PI_HOST) "cd $(PI_DIR) && docker-compose ps"
