package random.telegramhomebot.auth.listeners

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component
import random.telegramhomebot.auth.services.LoginAttemptService

@Component
class AuthenticationFailureListener(
    @Qualifier("loginAttemptService")
    private val loginAttemptService: LoginAttemptService
) : ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    override fun onApplicationEvent(e: AuthenticationFailureBadCredentialsEvent) {
        val auth = e.authentication.details as WebAuthenticationDetails
        loginAttemptService.loginFailed(auth.remoteAddress)
    }
}
