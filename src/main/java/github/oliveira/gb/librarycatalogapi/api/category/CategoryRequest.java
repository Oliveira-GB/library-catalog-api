package github.oliveira.gb.librarycatalogapi.api.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new category.
 * Uses Java Record for immutability and clean contract.
 *
 * @param name the category name (required, max 100 characters)
 */
public record CategoryRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name
) {
}
