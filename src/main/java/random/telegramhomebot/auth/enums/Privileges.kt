package random.telegramhomebot.auth.enums;

public enum Privileges {
    VIEW_COMMANDS("VIEW_COMMANDS"),
    ADD_COMMAND("ADD_COMMAND"),
    EDIT_COMMAND("EDIT_COMMAND"),
    DELETE_COMMAND("DELETE_COMMAND"),
    VIEW_HOSTS("VIEW_HOSTS"),
    IMPORT_CSV_HOSTS("IMPORT_CSV_HOSTS"),
    EXPORT_CSV_HOSTS("EXPORT_CSV_HOSTS"),
    ADD_HOST("ADD_HOST"),
    EDIT_HOST("EDIT_HOST"),
    DELETE_HOST("DELETE_HOST");

    private final String name;

    Privileges(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
