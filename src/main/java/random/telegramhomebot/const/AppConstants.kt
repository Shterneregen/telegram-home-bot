package random.telegramhomebot.const

import java.time.format.DateTimeFormatter

object AppConstants {
    const val ERROR_404_REDIRECT = "redirect:/404"

    const val DATE_PATTERN = "ddMMyyyy-hh-mm-ss"
    val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN)

    const val HOSTS_MAPPING = "/hosts"
    const val EDIT_HOST_MAPPING = "/edit"
    const val EDIT_HOST_BY_ID_MAPPING = "/edit/{id}"
    const val DELETE_HOST_MAPPING = "/delete/{id}"
    const val SAVE_HOST_MAPPING = "/save-host"
    const val REDIRECT_HOSTS = "redirect:" + HOSTS_MAPPING
    const val HOSTS_VIEW = "hosts"
    const val ADD_EDIT_HOST_VIEW = "add-edit-host"
    const val HOST_MODEL_ATTR = "host"
    const val HOSTS_MODEL_ATTR = "hosts"
    const val HOST_ID_PATH_VAR = "id"
    const val HOST_MAC_FIELD = "mac"

    const val TIME_LOG_MAPPING = "time-log"
    const val TIME_LOG_VIEW = "time-log"
    const val START_DATE_REQ_PARAM = "startDate"
    const val END_DATE_REQ_PARAM = "endDate"
    const val TIME_LOG_MAP_MODEL_ATTR = "timeLogMap"
    const val START_DATE_MODEL_ATTR = "startDate"
    const val END_DATE_MODEL_ATTR = "endDate"
    const val DEFAULT_DATE_VALUE_NOW = "#{T(java.time.LocalDateTime).now()}"

    const val HOSTS_CSV_EXPORT_MAPPING = "/export"
    const val HOSTS_CSV_IMPORT_MAPPING = "/import"
    const val FILE_REQ_PARAM = "file"

    const val COMMANDS_MAPPING = "/commands"
    const val EDIT_COMMAND_MAPPING = "/edit"
    const val EDIT_COMMAND_BY_ID_MAPPING = "/edit/{id}"
    const val DELETE_COMMAND_MAPPING = "/delete/{id}"
    const val SAVE_COMMAND_MAPPING = "/save-command"
    const val REDIRECT_COMMANDS = "redirect:$COMMANDS_MAPPING"
    const val ADD_EDIT_COMMAND_VIEW = "add-edit-command"
    const val COMMANDS_VIEW = "commands"
    const val COMMANDS_MODEL_ATTR = "commands"
    const val COMMAND_MODEL_ATTR = "command"
    const val COMMAND_ID_PATH_VAR = "id"

    const val NEW_HOSTS_MSG = "new.hosts"
    const val REACHABLE_HOSTS_MSG = "reachable.hosts"
    const val UNREACHABLE_HOSTS_MSG = "unreachable.hosts"
    const val UNAUTHORIZED_ACCESS_MSG = "unauthorized.access"
    const val CHATBOT_STARTED_MSG = "chatbot.started"
    const val HOST_MAC_NOT_UNIQUE_MSG = "host.mac.not.unique"

    const val REACHABLE_HOSTS_COMMAND = "/hosts"
    const val SHOW_ALL_COMMANDS = "/commands"
    const val LAST_ACTIVITY = "/activity"
    const val REFRESH = "/refresh"
}
