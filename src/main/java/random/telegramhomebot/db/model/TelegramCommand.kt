package random.telegramhomebot.db.model

import jakarta.persistence.*

@Entity
@Table(name = "telegram_command")
class TelegramCommand(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "telegram_command_seq")
    @SequenceGenerator(name = "telegram_command_seq", sequenceName = "telegram_command_seq", allocationSize = 1)
    @Column(updatable = false, nullable = false)
    var id: Long? = null,
    @Column(name = "command_alias", unique = true)
    var commandAlias: String = "",
    var command: String = "",
    var enabled: Boolean = false
) : Command {
    constructor(commandAlias: String, command: String, enabled: Boolean) : this(null, commandAlias, command, enabled)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val command = other as TelegramCommand
        return commandAlias == command.commandAlias
    }

    override fun hashCode(): Int {
        return commandAlias.hashCode()
    }

    override fun getButtonName(): String {
        return commandAlias
    }
}
