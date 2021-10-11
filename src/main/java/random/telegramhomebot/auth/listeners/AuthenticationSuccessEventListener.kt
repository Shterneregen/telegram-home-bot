package random.telegramhomebot.auth.listeners

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component
import random.telegramhomebot.auth.services.LoginAttemptService

@Component
class AuthenticationSuccessEventListener(
    @Qualifier("loginAttemptService")
    private val loginAttemptService: LoginAttemptService
) : ApplicationListener<AuthenticationSuccessEvent> {

    override fun onApplicationEvent(e: AuthenticationSuccessEvent) {
        val auth = e.authentication.details as WebAuthenticationDetails
        loginAttemptService.loginSucceeded(auth.remoteAddress)
    }
}
