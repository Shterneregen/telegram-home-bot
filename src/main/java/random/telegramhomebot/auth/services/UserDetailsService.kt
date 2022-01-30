package random.telegramhomebot.auth.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import random.telegramhomebot.auth.UserPrincipal
import random.telegramhomebot.auth.db.repositories.UserRepository
import random.telegramhomebot.services.messages.MessageService
import reactor.core.publisher.Mono

@Service
class UserDetailsService(
    private val userRepository: UserRepository,
    private val loginAttemptService: LoginAttemptService,
//    private val request: ServerHttpRequest,
    private val messageService: MessageService
) : ReactiveUserDetailsService {
// ) : MapReactiveUserDetailsService(users) {

    @Value("\${login.blocking.time.in.minutes}")
    private val blockingTimeInMinutes = 0

    override fun findByUsername(username: String): Mono<UserDetails> {
//        val ip = getClientIp(request)
//        if (loginAttemptService.isBlocked(ip)) {
//            throw RuntimeException(
//                messageService.getMessage(AuthErrorCode.USER_BLOCKED.messageCode, arrayOf(blockingTimeInMinutes / 60))
//            )
//        }
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(UsernameNotFoundException("Cannot find username: $username")))
            .flatMap { user -> Mono.just(UserPrincipal(user)) }
    }
}
