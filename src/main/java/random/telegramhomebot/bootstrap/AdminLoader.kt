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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

//        val adminPrivileges = Flux.merge(
//            viewCommands, addCommand, editCommand, deleteCommand,
//            viewHosts, importCsvHosts, exportCsvHosts, addHost, editHost, deleteHost
//        )
//        createRoleIfNotFound(AuthRole.ROLE_ADMIN.name, adminPrivileges.collectList().block(ofSeconds(10)))
//            .flatMap { adminRole -> createUserIfNotFound(adminLogin, adminPassword, "", "", "", listOf(adminRole)) }
//            .subscribe()

        Flux.merge(
            viewCommands, addCommand, editCommand, deleteCommand,
            viewHosts, importCsvHosts, exportCsvHosts, addHost, editHost, deleteHost
        )
            .collectList()
            .flatMap { adminPrivileges ->
                createRoleIfNotFound(AuthRole.ROLE_ADMIN.name, adminPrivileges)
            }.flatMap { adminRole ->
                createUserIfNotFound(adminLogin, adminPassword, "", "", "", listOf(adminRole))
            }.subscribe()

//        val adminRole = createRoleIfNotFound(
//            AuthRole.ROLE_ADMIN.name,
//            listOf(
//                viewCommands, addCommand, editCommand, deleteCommand,
//                viewHosts, importCsvHosts, exportCsvHosts, addHost, editHost, deleteHost
//            )
//        )
//        createUserIfNotFound(adminLogin, adminPassword, "", "", "", listOf(adminRole))

        Flux.merge(viewCommands, viewHosts)
            .collectList()
            .flatMap { userPrivileges ->
                createRoleIfNotFound(AuthRole.ROLE_USER.name, userPrivileges)
            }
            .flatMap { userRole ->
                createUserIfNotFound(userLogin, userPassword, "", "", "", listOf(userRole))
            }.subscribe()

//        val userRole = createRoleIfNotFound(AuthRole.ROLE_USER.name, listOf(viewCommands, viewHosts))
//        createUserIfNotFound(userLogin, userPassword, "", "", "", listOf(userRole))
    }

    @Transactional
    fun createPrivilegeIfNotFound(name: String): Mono<Privilege> {
        return privilegeRepository.findByName(name)
            .switchIfEmpty(privilegeRepository.save(Privilege(name)))
    }

    @Transactional
    fun createRoleIfNotFound(name: String, privileges: List<Privilege>): Mono<Role> {
        return roleRepository.findByName(name)
            .switchIfEmpty(Mono.just(Role(name, privileges)))
            .flatMap { role -> roleRepository.save(role) }
    }

    @Transactional
    fun createUserIfNotFound(
        username: String,
        password: String,
        email: String?,
        firstName: String?,
        lastName: String?,
        roles: List<Role>
    ): Mono<User> {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.just(User(username, password, firstName, lastName, email, true)))
            .doOnNext { user -> user.roles = roles }
            .flatMap { user -> userRepository.save(user) }
    }
}
