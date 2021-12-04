[![CircleCI](https://circleci.com/gh/Shterneregen/telegram-home-bot.svg?style=svg)](https://circleci.com/gh/Shterneregen/telegram-home-bot)

# Telegram Home Bot

Can be used for home automation on Raspberry Pi

## What can this bot do?

- Monitor local network and notify about hosts appearances/disappearances via Telegram client
- Run commands on target machine from Telegram client
- Wake-up selected hosts via WOL technology sending magic packet using Telegram client
- Provide weather info for chosen places using OpenWeatherMap API via Telegram client

## How to set up the project

### Create telegram chatbot

* Start a chat with **@BotFather**
* Use message **/newbot**
* Set unique bot name (ends with `_bot` or `Bot`, use it as **telegram.bot-name**)
* Get **token** from the final message (and use it as **telegram.token**)
* Start conversation with bot
* Retrieve **chat_id**, call https://api.telegram.org/bot[YOUR_TOKEN]/getUpdates
  (and use it as **telegram.bot-owner-id**)

### Enable network monitor

- Set `network-monitor.enabled` to `true`
- You can add/edit hosts on http://127.0.0.1:9988/hosts page
- Hosts availability changes is on http://127.0.0.1:9988/hosts page

#### What does network monitor do?

1. Periodically calls a command (**state.change.command**) to check network changes
2. Chatbot notifies about hosts appearances/disappearances **telegram.bot-owner-id** user

### Run commands on chatbot machine

1. You can add any initial commands that will be available to run
   in [commands.properties](src/main/resources/commands.properties)
2. Besides, you can add/edit commands on http://127.0.0.1:9988/commands

### Enable Weather menu

- Set `openweather.enabled` property to `true`
- Generate you API key on https://home.openweathermap.org/api_keys page
- Set the API key above as `openweather.appid` property
- Add locations where you want to know weather on page `/weather` page (http://127.0.0.1:9988/weather)
    - Don't add city ID with lat & lon in the same time, use them separately
    - City ID could be found in [city.list.json.gz](http://bulk.openweathermap.org/sample/city.list.json.gz)
      or [city.list.min.json.gz](http://bulk.openweathermap.org/sample/city.list.min.json.gz)
- After starting the app you can reach the Weather menu using `/weather` command via Telegram client
- Useful links
    - [Current weather data](https://openweathermap.org/current)
    - [Index of /sample/](http://bulk.openweathermap.org/sample/)

---

## Additional information

### [Installation as an init.d Service](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#deployment-initd-service)

* `gradlew clean bootJar`
* Copy a jar file to SOME_LINUX_FOLDER on linux machine

* On linux machine:

```shell
sudo mkdir /var/telegram # create a folder for jar file
sudo cp /SOME_LINUX_FOLDER/thb.jar /var/telegram/thb.jar # copy jar to the folder
sudo ln -s /var/telegram/thb.jar /etc/init.d/thb # create symlink the jar to init.d
sudo chmod +x /var/telegram/thb.jar # make thb.jar executable
sudo systemctl daemon-reload # reload systemd manager configuration
sudo service thb start # start bot as a service
update-rc.d thb defaults # autostart
```

* When I'm using THB as a Linux service, I just put the properties files next to the jar file and also set the Telegram
  credentials into the application.properties without using env variables

### Enable HTTPS

This example will be with a self-signed certificate on the local machine

* Create `thb-keystore.p12`

```shell
keytool -genkeypair -alias thb -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore thb-keystore.p12 -validity 3650 -ext san=ip:127.0.0.1
```

* Enter `SOME_SECURE_PSW` for keystore
* Uncomment `server.ssl.*` properties in the [application.properties](src/main/resources/application.properties)
  file (put your `SOME_SECURE_PSW` and change others if needed)
* Add `thb-keystore.p12` to the `Trusted Root Certification Authorities certificate` store

### Launch bot in Docker

```shell
# Build the image
docker build -t thb-image .

# Create and start new container from the image
docker run -d -p 80:8080 --network=bridge --name=thb  thb-image

# Start the container
docker start thb

# Stop the running container
docker stop thb

# Show information logged by a running container
docker logs -f thb
```

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

---

#### Create private chat group with chatbot (JFYI)

* open a session with __@BotFather__
* enter __/setjoingroups__
* enter the name of the bot
* enter Disable
* get private group __chat_id__, call https://api.telegram.org/bot$TOKEN/getUpdates

### Preview

![Commands](./screenshots/Commands.png)

![Hosts](./screenshots/Hosts.png)

![TimeLog](./screenshots/TimeLog.png)

---

Kotlin, Spring (Boot/Security/WebFlux), Gradle, H2, Thymeleaf, Google Charts, telegrambots-spring-boot-starter
