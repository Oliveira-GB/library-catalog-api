package github.oliveira.gb.librarycatalogapi.domain.admin;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom {@link UserDetailsService} implementation that retrieves and validates
 * administrative credentials from the database.
 *
 * <p>The role is injected statically as {@code ROLE_ADMIN} since the project
 * scope explicitly excludes RBAC and all users in the credential table are administrators.</p>
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final AdminUserRepository adminUserRepository;

    public DatabaseUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
                .username(adminUser.getUsername())
                .password(adminUser.getPassword())
                .roles(ADMIN_ROLE)
                .build();
    }
}
