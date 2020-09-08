# Telegram Home Bot

Can be used for home automation on Raspberry Pi

To use this project set environment variables for TELEGRAM_BOT_CHAT_ID, TELEGRAM_TOKEN and TELEGRAM_BOT_NAME


### Create telegram chatbot
* start a chat with __@BotFather__
* use message __/newbot__
* set bot name (TELEGRAM_BOT_NAME)
* set unique bot username (ends with _bot or Bot)
* get __token__ from the final message (TELEGRAM_TOKEN)
* start conversation with bot
* retrieve __chat_id__, call https://api.telegram.org/bot$TOKEN/getUpdates (TELEGRAM_BOT_CHAT_ID)

1. Periodically calls a command (_state.change.command_) to check network changes and in case of state changes sends a message to telegram bot (only to TELEGRAM_BOT_CHAT_ID)
2. You can add any commands in [commands.properties](src/main/resources/commands.properties). These commands work for TELEGRAM_BOT_CHAT_ID and users from __home.group.user.ids__ list

---

#### Create private chat group with chatbot (JFYI)
* open a session with __@BotFather__
* enter __/setjoingroups__
* enter the name of the bot
* enter Disable
* get private group __chat_id__, call https://api.telegram.org/bot$TOKEN/getUpdates
