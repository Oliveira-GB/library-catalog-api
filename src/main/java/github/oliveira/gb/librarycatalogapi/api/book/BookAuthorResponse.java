package github.oliveira.gb.librarycatalogapi.api.book;

/**
 * Response DTO for nested author information within a book response.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param id the author identifier
 * @param name the author name
 */
public record BookAuthorResponse(
        Long id,
        String name
) {
}
