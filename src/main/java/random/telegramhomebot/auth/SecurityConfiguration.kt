package random.telegramhomebot.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.context.request.RequestContextListener
import random.telegramhomebot.auth.enums.AuthRole
import random.telegramhomebot.auth.services.AppUserDetailsService

@Configuration
@EnableWebSecurity
class SecurityConfiguration(private val userDetailsService: AppUserDetailsService) : WebSecurityConfigurerAdapter() {

    @Value("\${bcrypt.rounds}")
    private val bcryptRounds = 0

    @Value("\${spring.h2.console.path}")
    private val springH2ConsolePath: String? = null

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        val urlsForAdmin = arrayOf(
            "$springH2ConsolePath**", "/actuator/**",
            "/commands/edit/*", "/commands/delete/*",
            "/hosts/edit/*", "/hosts/delete/*"
        )
        http
            .authorizeRequests()
            .antMatchers(*urlsForAdmin).hasAuthority(AuthRole.ROLE_ADMIN.name)
        super.configure(http)
    }

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        provider.setAuthoritiesMapper(authoritiesMapper())
        return provider
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(bcryptRounds)

    @Bean
    fun authoritiesMapper(): GrantedAuthoritiesMapper {
        val authorityMapper = SimpleAuthorityMapper()
        authorityMapper.setConvertToUpperCase(true)
        authorityMapper.setDefaultAuthority(AuthRole.ROLE_USER.name)
        return authorityMapper
    }

    @Bean
    fun requestContextListener() = RequestContextListener()

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(authenticationProvider())
    }
}
