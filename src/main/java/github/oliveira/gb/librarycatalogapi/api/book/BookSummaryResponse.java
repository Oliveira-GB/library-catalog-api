package github.oliveira.gb.librarycatalogapi.api.book;

import java.time.Instant;

/**
 * Lightweight response DTO for book list navigation.
 * Uses Java Record for immutability and clean serialization.
 * Omits heavy relationship data for performance.
 *
 * @param id the book identifier
 * @param title the book title
 * @param isbn the book ISBN
 * @param status the book status
 * @param categoryName the book category name
 * @param active whether the book is active
 * @param createdAt the creation timestamp
 * @param updatedAt the last update timestamp
 */
public record BookSummaryResponse(
        Long id,
        String title,
        String isbn,
        String status,
        String categoryName,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
