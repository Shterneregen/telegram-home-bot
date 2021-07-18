package random.telegramhomebot.auth;

public enum AuthGroups {
    USER("USER"),
    ADMIN("ADMIN");

    private final String authGroup;

    AuthGroups(String authGroup) {
        this.authGroup = authGroup;
    }

    public String getAuthGroup() {
        return authGroup;
    }
}
