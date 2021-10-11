package random.telegramhomebot.auth.enums

enum class AuthErrorCode(val errorCode: String, val messageCode: String) {
    USER_BLOCKED("USER_BLOCKED", "auth.message.blocked");
}
