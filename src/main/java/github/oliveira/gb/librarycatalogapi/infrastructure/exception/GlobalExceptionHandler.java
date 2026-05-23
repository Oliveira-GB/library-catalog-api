package github.oliveira.gb.librarycatalogapi.infrastructure.exception;

import github.oliveira.gb.librarycatalogapi.domain.book.exception.BookLoanedException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BatchLimitExceededException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BookUnavailableException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.DuplicateTitleException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.LoanAlreadyReturnedException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.MaxRenewalsReachedException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.OverdueBooksException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.PendingFinesException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.PossessionLimitExceededException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.TitleAlreadyLoanedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
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
     * Handles loan-specific business rule violations.
     * Returns HTTP 422 (Unprocessable Entity) for semantically valid requests
     * that violate transactional business constraints (batch limits, delinquency,
     * anti-hoarding, or availability).
     *
     * @param ex the loan validation exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure
     */
    @ExceptionHandler({
            BatchLimitExceededException.class,
            OverdueBooksException.class,
            PendingFinesException.class,
            DuplicateTitleException.class,
            TitleAlreadyLoanedException.class,
            BookUnavailableException.class,
            PossessionLimitExceededException.class,
            MaxRenewalsReachedException.class,
            LoanAlreadyReturnedException.class,
            BookLoanedException.class
    })
    public ProblemDetail handleLoanValidationException(RuntimeException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "loan-validation-failed"));
        problemDetail.setTitle("Loan Validation Failed");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        return problemDetail;
    }

    /**
     * Handles unsupported Accept headers during content negotiation.
     * Returns HTTP 406 with standardized problem detail.
     *
     * @param ex the media type not acceptable exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ProblemDetail handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_ACCEPTABLE,
                "The requested media type is not supported. Acceptable types: application/json, text/csv, application/pdf."
        );
        problemDetail.setType(URI.create(BASE_ERROR_URI + "media-type-not-acceptable"));
        problemDetail.setTitle("Media Type Not Acceptable");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));
        return problemDetail;
    }

    /**
     * Handles authentication failures as a fallback layer.
     * Returns HTTP 401 with standardized RFC 7807 problem detail.
     *
     * @param ex the authentication exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "unauthorized"));
        problemDetail.setTitle("Unauthorized");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        return problemDetail;
    }

    /**
     * Handles access denied failures as a fallback layer.
     * Returns HTTP 403 with standardized RFC 7807 problem detail.
     *
     * @param ex the access denied exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "forbidden"));
        problemDetail.setTitle("Forbidden");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        return problemDetail;
    }

    /**
     * Handles malformed or unreadable request bodies (e.g., invalid JSON).
     * Returns HTTP 400 with a sanitized RFC 7807 response.
     *
     * @param ex the message not readable exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request body is malformed or could not be read. Please verify the JSON syntax and data types."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "malformed-request"));
        problemDetail.setTitle("Malformed Request");
        problemDetail.setInstance(URI.create(extractRequestPath(request)));

        return problemDetail;
    }

    /**
     * Generic fallback handler for all unmapped exceptions.
     * Returns HTTP 500 with a sanitized RFC 7807 response.
     * Internal exception details are logged server-side but never exposed to the client.
     *
     * @param ex the unexpected exception
     * @param request the current web request
     * @return ProblemDetail with RFC 7807 structure and generic safe message
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        String requestPath = extractRequestPath(request);
        log.error("Unhandled exception occurred at {}", requestPath, ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred. Please try again later or contact support."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(requestPath));

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
