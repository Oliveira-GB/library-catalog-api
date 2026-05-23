package github.oliveira.gb.librarycatalogapi.infrastructure.exception.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Custom {@link AuthenticationEntryPoint} that returns RFC 7807 Problem Details
 * for HTTP 401 (Unauthorized) responses.
 *
 * <p>This handler ensures the API maintains a consistent error format across all
 * endpoints, including those protected by Spring Security filters.</p>
 */
@Component
public class SecurityProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String BASE_ERROR_URI = "https://api.library-catalog.com/errors/";

    private final ObjectMapper objectMapper;

    public SecurityProblemDetailsAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource."
        );

        problemDetail.setType(URI.create(BASE_ERROR_URI + "unauthorized"));
        problemDetail.setTitle("Unauthorized");
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
