package github.oliveira.gb.librarycatalogapi.domain.book.exception;

/**
 * Exception thrown when attempting to inactivate a book that is currently loaned.
 * The book must be returned (status changed to DISPONIVEL) before inactivation.
 */
public class BookLoanedException extends RuntimeException {

    public BookLoanedException(String message) {
        super(message);
    }
}
