package random.telegramhomebot.auth.exceptinos

class InvalidOldPasswordException : RuntimeException {
    constructor() : super()
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)

    companion object {
        private const val serialVersionUID = -8725338681447417728L
    }
}
