package github.oliveira.gb.librarycatalogapi.infrastructure.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler that intercepts and processes exceptions across all controllers.
 * Implements RFC 7807 (Problem Details for HTTP APIs) standard for consistent error responses.
 *
 * <p>This handler ensures:
 * <ul>
 *   <li>Standardized error responses following RFC 7807 format</li>
 *   <li>No sensitive information leakage (SQL details, constraint names)</li>
 *   <li>Detailed validation errors for client feedback</li>
 *   <li>Appropriate HTTP status codes for each exception type</li>
 * </ul>
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final String BASE_ERROR_URI = "https://api.library-catalog.com/errors/";

    /**
     * Handles validation errors from @Valid annotated request bodies.
     * Returns HTTP 400 with detailed field validation errors.
     *
     * @param ex the validation exception containing field errors
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure and fieldErrors extension
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed. Check the fieldErrors for details."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "validation-error"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<Map<String, String>> fieldErrorsList = fieldErrors.stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("field", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    return errorMap;
                })
                .toList();

        problemDetail.setProperty("fieldErrors", fieldErrorsList);

        return problemDetail;
    }

    /**
     * Handles resource not found scenarios.
     * Returns HTTP 404 with standardized problem detail.
     *
     * @param ex the resource not found exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "resource-not-found"));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        return problemDetail;
    }

    /**
     * Handles database integrity violations (unique constraints, foreign key violations).
     * Returns HTTP 409 with a sanitized message to prevent information leakage.
     *
     * <p><strong>Security Note:</strong> This handler deliberately sanitizes all database
     * error messages to prevent exposure of internal schema details, constraint names,
     * or SQL statements.
     *
     * @param ex the data integrity violation exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure and safe generic message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A data conflict occurred. The operation could not be completed due to a constraint violation."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "data-conflict"));
        problemDetail.setTitle("Data Conflict");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        return problemDetail;
    }

    /**
     * Extracts the request path from WebRequest description.
     * WebRequest.getDescription() returns "uri=/path", this method extracts just the path.
     *
     * @param request the web request
     * @return the request path (e.g., "/api/v1/books")
     */
    private String extractRequestPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}
