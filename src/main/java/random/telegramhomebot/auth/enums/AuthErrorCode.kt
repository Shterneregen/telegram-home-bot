package random.telegramhomebot.auth.enums;

public enum AuthErrorCode {

    USER_BLOCKED("USER_BLOCKED", "auth.message.blocked");

    private final String errorCode;
    private final String messageCode;

    AuthErrorCode(String errorCode, String messageCode) {
        this.errorCode = errorCode;
        this.messageCode = messageCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessageCode() {
        return messageCode;
    }
}
