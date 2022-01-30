package random.telegramhomebot.db.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.util.UUID
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "telegram_commands")
class TelegramCommand(
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    var id: UUID? = null,
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
