package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when the user has unpaid fines or fees.
 */
public class PendingFinesException extends RuntimeException {

    public PendingFinesException(String message) {
        super(message);
    }
}
