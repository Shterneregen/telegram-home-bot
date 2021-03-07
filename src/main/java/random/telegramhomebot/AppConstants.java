package random.telegramhomebot;

import java.time.format.DateTimeFormatter;

public interface AppConstants {

	String DATE_PATTERN = "ddMMyyyy-hh-mm-ss";
	DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

	interface Hosts {
		String HOSTS_MAPPING = "/hosts";
		String EDIT_HOST_MAPPING = "/edit";
		String EDIT_HOST_BY_ID_MAPPING = "/edit/{id}";
		String DELETE_HOST_MAPPING = "/delete/{id}";
		String SAVE_HOST_MAPPING = "/save-host";
		String REDIRECT_HOSTS = "redirect:" + HOSTS_MAPPING;

		String HOSTS_VIEW = "hosts";
		String ADD_EDIT_HOST_VIEW = "add-edit-host";

		String HOST_MODEL_ATTR = "host";
		String HOSTS_MODEL_ATTR = "hosts";
		String HOST_ID_PATH_VAR = "id";
		String HOST_MAC_FIELD = "mac";
	}

	interface HostsTimeLog {
		String TIME_LOG_MAPPING = "time-log";
		String TIME_LOG_VIEW = "time-log";
		String START_DATE_REQ_PARAM = "startDate";
		String END_DATE_REQ_PARAM = "endDate";
		String TIME_LOG_MAP_MODEL_ATTR = "timeLogMap";
		String START_DATE_MODEL_ATTR = "startDate";
		String END_DATE_MODEL_ATTR = "endDate";
		String DEFAULT_DATE_VALUE_NOW = "#{T(java.time.LocalDateTime).now()}";
	}

	interface HostsCsv {
		String HOSTS_CSV_EXPORT_MAPPING = "/export";
		String HOSTS_CSV_IMPORT_MAPPING = "/import";
		String FILE_REQ_PARAM = "file";
	}

	interface Commands {
		String COMMANDS_MAPPING = "/commands";
		String EDIT_COMMAND_MAPPING = "/edit";
		String EDIT_COMMAND_BY_ID_MAPPING = "/edit/{id}";
		String DELETE_COMMAND_MAPPING = "/delete/{id}";
		String SAVE_COMMAND_MAPPING = "/save-command";
		String REDIRECT_COMMANDS = "redirect:" + COMMANDS_MAPPING;

		String ADD_EDIT_COMMAND_VIEW = "add-edit-command";
		String COMMANDS_VIEW = "commands";

		String COMMANDS_MODEL_ATTR = "commands";
		String COMMAND_MODEL_ATTR = "command";
		String COMMAND_ID_PATH_VAR = "id";
	}

	interface Messages {
		String NEW_HOSTS_MSG = "new.hosts";
		String REACHABLE_HOSTS_MSG = "reachable.hosts";
		String UNREACHABLE_HOSTS_MSG = "unreachable.hosts";
		String UNAUTHORIZED_ACCESS_MSG = "unauthorized.access";
		String CHATBOT_STARTED_MSG = "chatbot.started";
		String HOST_MAC_NOT_UNIQUE_MSG = "host.mac.not.unique";
	}

	interface BotCommands {
		String SHOW_STORED_HOSTS_COMMAND = "/hosts";
		String SHOW_ALL_COMMANDS = "/commands";
		String MENU_COMMAND = "/menu";
		String LAST_ACTIVITY = "/activity";
		String FEATURES = "/features";
	}
}
