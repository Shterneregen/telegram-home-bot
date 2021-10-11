package random.telegramhomebot.auth.dto

import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

class GenericResponse {
    var message: String
    var error: String? = null

    constructor(message: String) {
        this.message = message
    }

    constructor(message: String, error: String?) {
        this.message = message
        this.error = error
    }

    constructor(allErrors: List<ObjectError>, error: String?) {
        this.error = error
        val temp = allErrors.map { e: ObjectError ->
            return@map if (e is FieldError) "{\"field\":\"${e.field}\",\"defaultMessage\":\"${e.getDefaultMessage()}\"}"
            else "{\"object\":\"${e.objectName}\",\"defaultMessage\":\"${e.defaultMessage}\"}"
        }.joinToString(separator = ",")
        message = "[$temp]"
    }
}
