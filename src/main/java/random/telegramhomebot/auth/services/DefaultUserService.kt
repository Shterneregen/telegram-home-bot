package random.telegramhomebot.auth.services

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import random.telegramhomebot.auth.db.entities.User
import random.telegramhomebot.auth.db.repositories.UserRepository
import reactor.core.publisher.Mono
import javax.transaction.Transactional

@Service
@Transactional
class DefaultUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    override fun getUserByID(id: Long): Mono<User> = userRepository.findById(id)

    override fun changeUserPassword(user: User, password: String) {
        user.password = passwordEncoder.encode(password)
        userRepository.save(user).block()
    }

    override fun checkIfValidOldPassword(user: User, oldPassword: String) =
        passwordEncoder.matches(oldPassword, user.password)
}
