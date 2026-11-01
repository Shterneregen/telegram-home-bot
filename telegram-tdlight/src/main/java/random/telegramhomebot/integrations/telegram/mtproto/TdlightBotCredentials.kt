package random.telegramhomebot.integrations.telegram.mtproto

/** Immutable transport inputs. Deliberately has no generated toString containing the token. */
class TdlightBotCredentials(val token: String, val chatId: Long)
