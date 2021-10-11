package random.telegramhomebot.auth.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import random.telegramhomebot.auth.UserPrincipal
import random.telegramhomebot.auth.db.repositories.UserRepository
import random.telegramhomebot.auth.enums.AuthErrorCode
import random.telegramhomebot.services.messages.MessageService
import random.telegramhomebot.utils.NetUtils.Companion.getClientIp
import javax.servlet.http.HttpServletRequest

@Service
class AppUserDetailsService(
    private val userRepository: UserRepository,
    private val loginAttemptService: LoginAttemptService,
    private val request: HttpServletRequest,
    private val messageService: MessageService
) : UserDetailsService {

    @Value("\${login.blocking.time.in.minutes}")
    private val blockingTimeInMinutes = 0

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val ip = getClientIp(request)
        if (loginAttemptService.isBlocked(ip)) {
            throw RuntimeException(
                messageService.getMessage(AuthErrorCode.USER_BLOCKED.messageCode, arrayOf(blockingTimeInMinutes / 60))
            )
        }
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("Cannot find username: $username")
        return UserPrincipal(user)
    }
}
