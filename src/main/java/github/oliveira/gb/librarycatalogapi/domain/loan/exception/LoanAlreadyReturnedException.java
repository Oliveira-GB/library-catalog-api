package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when an operation is attempted on a loan that has already
 * been returned and finalized.
 */
public class LoanAlreadyReturnedException extends RuntimeException {

    public LoanAlreadyReturnedException(String message) {
        super(message);
    }
}
