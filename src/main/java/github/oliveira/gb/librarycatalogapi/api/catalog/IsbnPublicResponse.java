package github.oliveira.gb.librarycatalogapi.api.catalog;

/**
 * Public DTO for ISBN lookup endpoint.
 * Exposes no primary key or stock quantity for security isolation.
 * Uses Java Record for immutability and clean serialization.
 *
 * @param title the book title
 * @param author concatenated author names
 * @param genre the category name
 * @param isbn the book ISBN
 */
public record IsbnPublicResponse(
        String title,
        String author,
        String genre,
        String isbn
) {
}
