package random.telegramhomebot.auth.services

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import random.telegramhomebot.auth.db.entities.User
import random.telegramhomebot.auth.db.repositories.UserRepository
import javax.transaction.Transactional

@Service
@Transactional
class DefaultUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    override fun getUserByID(id: Long): User? = userRepository.findById(id).orElse(null)

    override fun changeUserPassword(user: User, password: String) {
        user.password = passwordEncoder.encode(password)
        userRepository.save(user)
    }

    override fun checkIfValidOldPassword(user: User, oldPassword: String) =
        passwordEncoder.matches(oldPassword, user.password)
}
