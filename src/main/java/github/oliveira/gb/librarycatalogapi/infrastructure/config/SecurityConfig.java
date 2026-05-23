package github.oliveira.gb.librarycatalogapi.infrastructure.config;

import github.oliveira.gb.librarycatalogapi.domain.admin.DatabaseUserDetailsService;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.security.SecurityProblemDetailsAccessDeniedHandler;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.security.SecurityProblemDetailsAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration providing database-backed Basic Auth for administrative routes
 * and permitting public access to the ISBN catalog lookup endpoint.
 *
 * <p>CSRF is safely disabled because this is a stateless headless REST API using HTTP
 * Basic Auth. Basic Auth sends credentials explicitly in every request via the
 * Authorization header, not via session cookies. Without session cookies, there is no
 * cross-site request forgery (CSRF) attack surface.</p>
 *
 * <p>See OWASP REST Security Cheat Sheet for justification:</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DatabaseUserDetailsService databaseUserDetailsService;
    private final SecurityProblemDetailsAuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityProblemDetailsAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(DatabaseUserDetailsService databaseUserDetailsService,
                          SecurityProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
                          SecurityProblemDetailsAccessDeniedHandler accessDeniedHandler) {
        this.databaseUserDetailsService = databaseUserDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/livros/**").permitAll()
                .requestMatchers("/api/v1/relatorios/**").authenticated()
                .anyRequest().authenticated()
            )
            .userDetailsService(databaseUserDetailsService)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
