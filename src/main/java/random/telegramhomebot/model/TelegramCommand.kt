package random.telegramhomebot.model

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.*

@Entity
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

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val command = o as TelegramCommand
        return commandAlias == command.commandAlias
    }

    override fun hashCode(): Int {
        return commandAlias.hashCode()
    }

    override fun getButtonName(): String {
        return commandAlias
    }
}
