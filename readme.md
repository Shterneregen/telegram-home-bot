# telegram-network-client-notifier

Periodically calls "arp -a" command and in case of state changing sends a message to telegram bot

### To create chat-bot
* start a chat with __@BotFather__
* use message __/newbot__
* set bot name
* set unique bot username (ends with _bot or Bot)
* get __token__ from final message
* start conversation with bot
* retrieve __chat_id__, call https://api.telegram.org/bot$TOKEN/getUpdates 
* set TELEGRAM_CHAT_ID and TELEGRAM_TOKEN as environment variables
