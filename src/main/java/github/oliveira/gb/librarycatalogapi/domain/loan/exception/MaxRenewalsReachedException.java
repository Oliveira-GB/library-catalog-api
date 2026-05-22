package github.oliveira.gb.librarycatalogapi.domain.loan.exception;

/**
 * Exception thrown when a renewal request is made for a loan that has already
 * reached the maximum allowed consecutive renewals.
 */
public class MaxRenewalsReachedException extends RuntimeException {

    public MaxRenewalsReachedException(String message) {
        super(message);
    }
}
