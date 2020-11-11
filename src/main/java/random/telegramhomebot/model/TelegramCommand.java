package random.telegramhomebot.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "telegram_commands")
public class TelegramCommand {

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

	public TelegramCommand() {
	}

	public TelegramCommand(String commandAlias, String command) {
		this.commandAlias = commandAlias;
		this.command = command;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCommandAlias() {
		return commandAlias;
	}

	public void setCommandAlias(String commandAlias) {
		this.commandAlias = commandAlias;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
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
}
