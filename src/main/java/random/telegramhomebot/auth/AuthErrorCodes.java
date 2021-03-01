package random.telegramhomebot.auth;

public enum AuthErrorCodes {

	USER_BLOCKED("USER_BLOCKED", "auth.message.blocked");

	private final String errorCode;
	private final String errorMessageCode;

	AuthErrorCodes(String errorCode, String errorMessageCode) {
		this.errorCode = errorCode;
		this.errorMessageCode = errorMessageCode;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessageCode() {
		return errorMessageCode;
	}
}
