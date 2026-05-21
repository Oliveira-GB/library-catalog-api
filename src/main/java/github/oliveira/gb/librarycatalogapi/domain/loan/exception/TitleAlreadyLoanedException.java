package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when the user already holds an active loan of a title
 * present in the requested batch (anti-hoarding rule).
 */
public class TitleAlreadyLoanedException extends RuntimeException {

    public TitleAlreadyLoanedException(String message) {
        super(message);
    }
}
