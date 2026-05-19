package github.oliveira.gb.librarycatalogapi.api.author;

import java.time.Instant;

/**
 * Response DTO for author data.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param id the author identifier
 * @param name the author name
 * @param email the author email
 * @param biography the author biography
 * @param active whether the author is active
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
public record AuthorResponse(
        Long id,
        String name,
        String email,
        String biography,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
