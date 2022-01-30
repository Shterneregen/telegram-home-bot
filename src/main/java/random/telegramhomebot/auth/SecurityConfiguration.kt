package random.telegramhomebot.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import random.telegramhomebot.auth.enums.AuthRole

@Configuration
// @EnableWebSecurity
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration/*(private val userDetailsService: UserDetailsService): WebSecurityConfigurerAdapter()*/ {

    @Value("\${bcrypt.rounds}")
    private lateinit var bcryptRounds: Number

    @Value("\${spring.h2.console.path}")
    private val springH2ConsolePath: String? = null

//    @Throws(Exception::class)
//    override fun configure(http: HttpSecurity) {
//        val urlsForAdmin = arrayOf(
//            "$springH2ConsolePath**", "/actuator/**",
//            "/commands/edit/*", "/commands/delete/*",
//            "/hosts/edit/*", "/hosts/delete/*"
//        )
//        http
//            .authorizeRequests()
//            .antMatchers(*urlsForAdmin).hasAuthority(AuthRole.ROLE_ADMIN.name)
//        super.configure(http)
//    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        val urlsForAdmin = arrayOf(
            "$springH2ConsolePath**", "/actuator/**",
            "/commands/edit/*", "/commands/delete/*",
            "/hosts/edit/*", "/hosts/delete/*"
        )

        http.authorizeExchange()
            .pathMatchers(*urlsForAdmin).hasAuthority(AuthRole.ROLE_ADMIN.name)
            .anyExchange().authenticated()
            .and().httpBasic()
        return http.build()
    }

    @Bean
    fun authenticationProvider(): ReactiveAuthenticationManager {
//        val provider = DaoAuthenticationProvider()
//        provider.setUserDetailsService(userDetailsService)
//        provider.setPasswordEncoder(passwordEncoder())
//        provider.setAuthoritiesMapper(authoritiesMapper())
//        return provider

        val provider = DaoAuthenticationProvider()
//        provider.setUserDetailsService(userDetailsService)
//        provider.setPasswordEncoder(passwordEncoder())
//        provider.setAuthoritiesMapper(authoritiesMapper())
        return ReactiveAuthenticationManagerAdapter(ProviderManager(listOf(provider)))
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(bcryptRounds.toInt())

    @Bean
    fun authoritiesMapper(): GrantedAuthoritiesMapper {
        val authorityMapper = SimpleAuthorityMapper()
        authorityMapper.setConvertToUpperCase(true)
        authorityMapper.setDefaultAuthority(AuthRole.ROLE_USER.name)
        return authorityMapper
    }

//    @Bean
//    fun requestContextListener() = RequestContextListener()

//    override fun configure(auth: AuthenticationManagerBuilder) {
//        auth.authenticationProvider(authenticationProvider())
//    }
}
