package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when a requested book is unavailable for loan
 * (status is not DISPONIVEL).
 */
public class BookUnavailableException extends RuntimeException {

    public BookUnavailableException(String message) {
        super(message);
    }
}
