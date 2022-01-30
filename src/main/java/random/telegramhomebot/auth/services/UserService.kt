package random.telegramhomebot.auth.services

import random.telegramhomebot.auth.db.entities.User
import reactor.core.publisher.Mono

interface UserService {
    fun getUserByID(id: Long): Mono<User>
    fun changeUserPassword(user: User, password: String)
    fun checkIfValidOldPassword(user: User, oldPassword: String): Boolean
}
