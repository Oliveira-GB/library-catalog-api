package github.oliveira.gb.librarycatalogapi.api.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.ISBN;

import java.util.List;

/**
 * Request DTO for creating or updating a book.
 * Uses Java Record for immutability and clean contract.
 *
 * @param title the book title (required, max 200 characters)
 * @param isbn the book ISBN (required, must be valid format, max 20 characters)
 * @param categoryId the category identifier (required)
 * @param authorIds the list of author identifiers (required, at least one)
 */
public record BookRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        String title,

        @NotBlank(message = "ISBN is required")
        @ISBN(message = "ISBN must be a valid ISBN-10 or ISBN-13")
        @Size(max = 25, message = "ISBN must not exceed 25 characters")
        String isbn,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotEmpty(message = "At least one author is required")
        List<@NotNull(message = "Author ID cannot be null") Long> authorIds
) {
}
