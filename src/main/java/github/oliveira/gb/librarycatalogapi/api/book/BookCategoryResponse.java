package github.oliveira.gb.librarycatalogapi.api.book;

/**
 * Response DTO for nested category information within a book response.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param id the category identifier
 * @param name the category name
 */
public record BookCategoryResponse(
        Long id,
        String name
) {
}
