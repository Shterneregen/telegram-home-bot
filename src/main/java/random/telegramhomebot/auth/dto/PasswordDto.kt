package random.telegramhomebot.auth.dto

import random.telegramhomebot.auth.validation.ValidPassword

class PasswordDto(
    var oldPassword: String,
    @ValidPassword
    var newPassword: String
) {
    var token: String? = null
}
