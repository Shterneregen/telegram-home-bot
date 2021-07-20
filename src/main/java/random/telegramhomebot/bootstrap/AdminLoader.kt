package random.telegramhomebot.bootstrap

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import random.telegramhomebot.auth.entities.Privilege
import random.telegramhomebot.auth.entities.Role
import random.telegramhomebot.auth.entities.User
import random.telegramhomebot.auth.repositories.PrivilegeRepository
import random.telegramhomebot.auth.repositories.RoleRepository
import random.telegramhomebot.auth.repositories.UserRepository
import random.telegramhomebot.utils.logger

@Component
class AdminLoader(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val privilegeRepository: PrivilegeRepository
) : CommandLineRunner {
    val log = logger()

    @Value("\${default.admin.login}")
    private lateinit var adminLogin: String

    @Value("\${default.admin.password}")
    private lateinit var adminPassword: String

    @Value("\${default.user.login}")
    private lateinit var userLogin: String

    @Value("\${default.user.password}")
    private lateinit var userPassword: String

    override fun run(vararg args: String) {
        createAdminUser()
    }

    private fun createAdminUser() {
        val viewCommands = createPrivilegeIfNotFound("VIEW_COMMANDS")
        val addCommand = createPrivilegeIfNotFound("ADD_COMMAND")
        val editCommand = createPrivilegeIfNotFound("EDIT_COMMAND")
        val deleteCommand = createPrivilegeIfNotFound("DELETE_COMMAND")

        val viewHosts = createPrivilegeIfNotFound("VIEW_HOSTS")
        val importCsvHosts = createPrivilegeIfNotFound("IMPORT_CSV_HOSTS")
        val exportCsvHosts = createPrivilegeIfNotFound("EXPORT_CSV_HOSTS")
        val addHost = createPrivilegeIfNotFound("ADD_HOST")
        val editHost = createPrivilegeIfNotFound("EDIT_HOST")
        val deleteHost = createPrivilegeIfNotFound("DELETE_HOST")

        val adminRole = createRoleIfNotFound(
            "ROLE_ADMIN",
            listOf(
                viewCommands, addCommand, editCommand, deleteCommand,
                viewHosts, importCsvHosts, exportCsvHosts, addHost, editHost, deleteHost
            )
        )
        createUserIfNotFound(adminLogin, adminPassword, "", "", "", listOf(adminRole))

        val userRole = createRoleIfNotFound(
            "ROLE_USER",
            listOf(viewCommands, viewHosts)
        )
        createUserIfNotFound(userLogin, userPassword, "", "", "", listOf(userRole))
    }

    @Transactional
    fun createPrivilegeIfNotFound(name: String): Privilege? {
        var privilege: Privilege? = privilegeRepository.findByName(name)
        if (privilege == null) {
            privilege = Privilege(name)
            privilegeRepository.save(privilege)
        }
        return privilege
    }

    @Transactional
    fun createRoleIfNotFound(name: String?, privileges: Collection<Privilege?>?): Role? {
        val role: Role = roleRepository.findByName(name) ?: Role(name, privileges)
        return roleRepository.save(role)
    }

    @Transactional
    fun createUserIfNotFound(
        username: String,
        password: String?,
        email: String?,
        firstName: String?,
        lastName: String?,
        roles: Collection<Role?>
    ): User? {
        val user: User = userRepository.findByUsername(username)
            ?: User(username, password, firstName, lastName, email, true)
        user.roles = roles
        return userRepository.save(user)
    }
}