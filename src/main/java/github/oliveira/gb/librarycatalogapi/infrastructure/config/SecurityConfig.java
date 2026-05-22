package github.oliveira.gb.librarycatalogapi.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration providing Basic Auth for administrative routes
 * and permitting public access to the ISBN catalog lookup endpoint.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    private final String adminPassword;

    public SecurityConfig(@Value("${app.security.admin-password}") String adminPassword) {
        this.adminPassword = adminPassword;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF is safely disabled because this is a stateless headless REST API
            // using HTTP Basic Auth. Basic Auth sends credentials explicitly in every
            // request via the Authorization header, not via session cookies. Without
            // session cookies, there is no cross-site request forgery (CSRF) attack
            // surface. See OWASP: https://cheatsheetseries.owasp.org/cheatsheets/
            // REST_Security_Cheat_Sheet.html#csrf
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/catalogo/livros/**").permitAll()
                .requestMatchers("/api/v1/relatorios/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode(adminPassword))
            .roles(ADMIN_ROLE)
            .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
