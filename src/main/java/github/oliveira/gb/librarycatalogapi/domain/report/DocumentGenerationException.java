package github.oliveira.gb.librarycatalogapi.domain.report;

/**
 * Runtime exception thrown when document generation (CSV or PDF) fails.
 */
public class DocumentGenerationException extends RuntimeException {

    public DocumentGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentGenerationException(String message) {
        super(message);
    }
}
