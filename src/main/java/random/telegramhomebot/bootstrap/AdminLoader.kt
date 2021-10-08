package random.telegramhomebot.bootstrap

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import random.telegramhomebot.auth.db.entities.Privilege
import random.telegramhomebot.auth.db.entities.Role
import random.telegramhomebot.auth.db.entities.User
import random.telegramhomebot.auth.db.repositories.PrivilegeRepository
import random.telegramhomebot.auth.db.repositories.RoleRepository
import random.telegramhomebot.auth.db.repositories.UserRepository
import random.telegramhomebot.auth.enums.AuthRole
import random.telegramhomebot.auth.enums.Privileges
import random.telegramhomebot.utils.logger

@Order(1)
@Component
class AdminLoader(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val privilegeRepository: PrivilegeRepository
) : CommandLineRunner {
    private val log = logger()

    @Value("\${default.admin.login}")
    private lateinit var adminLogin: String

    @Value("\${default.admin.password}")
    private lateinit var adminPassword: String

    @Value("\${default.user.login}")
    private lateinit var userLogin: String

    @Value("\${default.user.password}")
    private lateinit var userPassword: String

    override fun run(vararg args: String) {
        log.info("AdminLoader started")
        createAdminUser()
    }

    private fun createAdminUser() {
        val viewCommands = createPrivilegeIfNotFound(Privileges.VIEW_COMMANDS.name)
        val addCommand = createPrivilegeIfNotFound(Privileges.ADD_COMMAND.name)
        val editCommand = createPrivilegeIfNotFound(Privileges.EDIT_COMMAND.name)
        val deleteCommand = createPrivilegeIfNotFound(Privileges.DELETE_COMMAND.name)

        val viewHosts = createPrivilegeIfNotFound(Privileges.VIEW_HOSTS.name)
        val importCsvHosts = createPrivilegeIfNotFound(Privileges.IMPORT_CSV_HOSTS.name)
        val exportCsvHosts = createPrivilegeIfNotFound(Privileges.EXPORT_CSV_HOSTS.name)
        val addHost = createPrivilegeIfNotFound(Privileges.ADD_HOST.name)
        val editHost = createPrivilegeIfNotFound(Privileges.EDIT_HOST.name)
        val deleteHost = createPrivilegeIfNotFound(Privileges.DELETE_HOST.name)

        val adminRole = createRoleIfNotFound(
            AuthRole.ADMIN.name,
            listOf(
                viewCommands, addCommand, editCommand, deleteCommand,
                viewHosts, importCsvHosts, exportCsvHosts, addHost, editHost, deleteHost
            )
        )
        createUserIfNotFound(adminLogin, adminPassword, "", "", "", listOf(adminRole))

        val userRole = createRoleIfNotFound(
            AuthRole.USER.name,
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