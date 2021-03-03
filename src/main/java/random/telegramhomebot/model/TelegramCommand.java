package random.telegramhomebot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "telegram_commands")
public class TelegramCommand implements Command {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Type(type = "org.hibernate.type.UUIDCharType")
	@Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
	private UUID id;
	@Column(name = "command_alias", unique = true)
	private String commandAlias;
	@Column(name = "command")
	private String command;
	@Column(name = "enabled")
	private Boolean enabled;

	public TelegramCommand(String commandAlias, String command) {
		this.commandAlias = commandAlias;
		this.command = command;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TelegramCommand command = (TelegramCommand) o;

		return Objects.equals(commandAlias, command.commandAlias);
	}

	@Override
	public int hashCode() {
		return commandAlias != null ? commandAlias.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "TelegramCommand{" +
				"id=" + id +
				", commandAlias='" + commandAlias + '\'' +
				", command='" + command + '\'' +
				", enabled=" + enabled +
				'}';
	}

	@Override
	public String getButtonName() {
		return getCommandAlias();
	}
}
