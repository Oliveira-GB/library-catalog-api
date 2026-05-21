package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when the user has overdue books from previous transactions.
 */
public class OverdueBooksException extends RuntimeException {

    public OverdueBooksException(String message) {
        super(message);
    }
}
