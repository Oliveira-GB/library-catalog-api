package github.oliveira.gb.librarycatalogapi.api.catalog;

/**
 * Flat projection DTO for catalog discovery search results.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param id the book identifier
 * @param title the book title
 * @param author concatenated author names
 * @param genre the category name
 * @param isbn the book ISBN
 * @param available true if book status is DISPONIVEL
 */
public record CatalogSearchResponse(
        Long id,
        String title,
        String author,
        String genre,
        String isbn,
        Boolean available
) {
}
