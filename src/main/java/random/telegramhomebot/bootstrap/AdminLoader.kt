package random.telegramhomebot.bootstrap

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import random.telegramhomebot.auth.entities.AuthGroup
import random.telegramhomebot.auth.entities.User
import random.telegramhomebot.auth.repositories.AuthGroupRepository
import random.telegramhomebot.auth.repositories.UserRepository
import random.telegramhomebot.utils.logger

@Component
class AdminLoader(
    private val userRepository: UserRepository,
    private val authGroupRepository: AuthGroupRepository
) : CommandLineRunner {
    val log = logger()

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