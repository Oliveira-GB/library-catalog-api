package github.oliveira.gb.librarycatalogapi.api.report;

/**
 * Projection DTO for the inventory report.
 * Represents individual physical book units (no BookCopy entity exists).
 * Uses Java Record for immutability and clean serialization.
 *
 * @param bookId the book identifier
 * @param title the book title
 * @param isbn the book ISBN
 * @param status the book status (DISPONIVEL or EMPRESTADO)
 */
public record InventoryReportResponse(
        Long bookId,
        String title,
        String isbn,
        String status
) {
}
