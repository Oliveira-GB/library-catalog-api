package github.oliveira.gb.librarycatalogapi.api.category;

import java.time.Instant;

/**
 * Response DTO for category data.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param id the category identifier
 * @param name the category name
 * @param active whether the category is active
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
public record CategoryResponse(
        Long id,
        String name,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
