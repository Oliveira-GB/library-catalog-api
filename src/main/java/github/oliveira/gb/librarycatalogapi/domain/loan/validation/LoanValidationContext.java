package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;

import java.time.Instant;
import java.util.List;

/**
 * Immutable context object passed to all loan validators during the validation phase.
 * Carries the locked reader, the raw requested book IDs, the resolved book entities,
 * and the current instant for time-based checks (e.g., overdue validation).
 */
public record LoanValidationContext(Reader reader, List<Long> bookIds, List<Book> books, Instant now) {
}
