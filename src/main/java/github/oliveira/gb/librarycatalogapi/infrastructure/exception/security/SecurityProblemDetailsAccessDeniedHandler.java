package github.oliveira.gb.librarycatalogapi.infrastructure.exception.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Custom {@link AccessDeniedHandler} that returns RFC 7807 Problem Details
 * for HTTP 403 (Forbidden) responses.
 *
 * <p>This handler ensures the API maintains a consistent error format across all
 * endpoints, including those protected by Spring Security filters.</p>
 */
@Component
public class SecurityProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private static final String BASE_ERROR_URI = "https://api.library-catalog.com/errors/";

    private final ObjectMapper objectMapper;

    public SecurityProblemDetailsAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "forbidden"));
        problemDetail.setTitle("Forbidden");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
