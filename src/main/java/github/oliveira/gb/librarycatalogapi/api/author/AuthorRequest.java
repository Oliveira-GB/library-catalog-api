package github.oliveira.gb.librarycatalogapi.api.author;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new author.
 * Uses Java Record for immutability and clean contract.
 *
 * @param name the author name (required, max 150 characters)
 * @param email the author email (required, must be valid format, max 150 characters)
 * @param biography the author biography (optional, max 2000 characters)
 */
public record AuthorRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 2000, message = "Biography must not exceed 2000 characters")
        String biography
) {
}
