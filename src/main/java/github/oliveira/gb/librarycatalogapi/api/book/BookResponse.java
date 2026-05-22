package github.oliveira.gb.librarycatalogapi.api.book;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for book detail data.
 * Uses Java Record for immutability and clean serialization.
 * Includes full nested category and authors information.
 *
 * @param id the book identifier
 * @param title the book title
 * @param isbn the book ISBN
 * @param status the book status
 * @param category the book category
 * @param authors the list of book authors
 * @param active whether the book is active
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
public record BookResponse(
        Long id,
        String title,
        String isbn,
        String status,
        BookCategoryResponse category,
        List<BookAuthorResponse> authors,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
