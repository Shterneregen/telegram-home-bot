package random.telegramhomebot.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import random.telegramhomebot.auth.db.entities.Role
import random.telegramhomebot.auth.db.entities.User

class UserPrincipal(private val user: User) : UserDetails {

    val id: Long
        get() = user.id ?: throw RuntimeException("Cannot get user id")

    override fun getAuthorities(): List<GrantedAuthority> = getAuthorities(user.roles ?: emptyList())

    private fun getAuthorities(roles: List<Role>) = getGrantedAuthorities(getPrivileges(roles))
    private fun getGrantedAuthorities(privileges: List<String>) = privileges.map { SimpleGrantedAuthority(it) }
    private fun getPrivileges(roles: Collection<Role>): List<String> =
        roles.flatMap { it.privileges }.map { it.name } + roles.map { it.name }

    override fun getPassword() = user.password
    override fun getUsername() = user.username
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
