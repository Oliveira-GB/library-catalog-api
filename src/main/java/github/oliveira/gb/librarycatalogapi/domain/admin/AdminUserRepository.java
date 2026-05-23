package github.oliveira.gb.librarycatalogapi.domain.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link AdminUser} providing secure credential lookup.
 */
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * Finds an active admin user by username.
     *
     * @param username the username to search for
     * @return an Optional containing the admin user if found
     */
    Optional<AdminUser> findByUsername(String username);
}
