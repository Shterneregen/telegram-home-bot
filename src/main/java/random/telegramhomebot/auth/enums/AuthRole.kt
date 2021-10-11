package random.telegramhomebot.auth.enums;

public enum AuthRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String name;

    AuthRole(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
