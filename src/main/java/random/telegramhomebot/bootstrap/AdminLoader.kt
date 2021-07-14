package random.telegramhomebot.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import random.telegramhomebot.auth.entities.AuthGroup
import random.telegramhomebot.auth.entities.User
import random.telegramhomebot.auth.repositories.AuthGroupRepository
import random.telegramhomebot.auth.repositories.UserRepository

@Component
class AdminLoader(
    private val userRepository: UserRepository,
    private val authGroupRepository: AuthGroupRepository
) : CommandLineRunner {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @Value("\${default.admin.login}")
    private lateinit var adminLogin: String

    @Value("\${default.admin.password}")
    private lateinit var adminPassword: String

    override fun run(vararg args: String) {
        createAdminUser()
    }

    private fun createAdminUser() {
        if (userRepository.count() == 0L) {
            log.info("Creating admin user [{}]", adminLogin)
            userRepository.save(User(adminLogin, adminPassword))
            authGroupRepository.saveAll(
                listOf(
                    AuthGroup(adminLogin, "USER"),
                    AuthGroup(adminLogin, "ADMIN")
                )
            )
        }
    }
}