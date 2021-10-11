package random.telegramhomebot.auth.services

import random.telegramhomebot.auth.db.entities.User

interface UserService {
    fun getUserByID(id: Long): User?
    fun changeUserPassword(user: User, password: String)
    fun checkIfValidOldPassword(user: User, oldPassword: String): Boolean
}
