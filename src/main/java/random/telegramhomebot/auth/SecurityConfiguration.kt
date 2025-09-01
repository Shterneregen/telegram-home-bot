package random.telegramhomebot.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import random.telegramhomebot.auth.enums.AuthRole
import random.telegramhomebot.auth.services.AppUserDetailsService

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(private val userDetailsService: AppUserDetailsService) {

    @Value("\${bcrypt.rounds}")
    private val bcryptRounds = 0

    @Value("\${spring.h2.console.path}")
    private val springH2ConsolePath: String? = null

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "$springH2ConsolePath/**",
                        "/actuator/**",
                        "/commands/edit/**",
                        "/commands/delete/**",
                        "/hosts/edit/**",
                        "/hosts/delete/**"
                    ).hasAuthority(AuthRole.ROLE_ADMIN.name)
                    .requestMatchers(
                        "/.well-known/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { it.permitAll() }
            .csrf { it.disable() }
            .headers { it.frameOptions { options -> options.disable() } }
        return http.build()
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
}
