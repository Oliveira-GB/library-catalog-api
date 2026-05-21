package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when the requested loan batch exceeds the allowed size (1-5 books).
 */
public class BatchLimitExceededException extends RuntimeException {

    public BatchLimitExceededException(String message) {
        super(message);
    }
}
