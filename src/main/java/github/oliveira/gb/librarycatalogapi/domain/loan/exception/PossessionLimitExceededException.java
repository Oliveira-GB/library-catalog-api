package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when a reader attempts to borrow more books than the cumulative
 * possession limit allows. The global rigid limit is 5 active books per reader.
 */
public class PossessionLimitExceededException extends RuntimeException {

    public PossessionLimitExceededException(String message) {
        super(message);
    }
}
