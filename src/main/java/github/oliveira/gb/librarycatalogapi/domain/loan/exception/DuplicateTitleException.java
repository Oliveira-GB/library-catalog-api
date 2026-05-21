package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when the requested batch contains duplicate titles.
 */
public class DuplicateTitleException extends RuntimeException {

    public DuplicateTitleException(String message) {
        super(message);
    }
}
