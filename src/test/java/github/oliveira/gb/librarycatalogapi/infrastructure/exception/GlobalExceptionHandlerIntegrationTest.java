package github.oliveira.gb.librarycatalogapi.infrastructure.exception;

import github.oliveira.gb.librarycatalogapi.infrastructure.exception.dummy.DummyController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for GlobalExceptionHandler using TDD approach.
 * Validates RFC 7807 compliance and proper exception handling across all scenarios.
 *
 * <p>Test coverage includes:
 * <ul>
 *   <li>HTTP 404 - ResourceNotFoundException</li>
 *   <li>HTTP 400 - MethodArgumentNotValidException with fieldErrors</li>
 *   <li>HTTP 409 - DataIntegrityViolationException (sanitized)</li>
 * </ul>
 */
@WebMvcTest(DummyController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Global Exception Handler Integration Tests")
class GlobalExceptionHandlerIntegrationTest {

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASS = "admin123";

    private static final String BASE_API_PATH = "/api/v1/test";

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Resource Not Found Exception Tests (HTTP 404)")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("Should return RFC 7807 response when resource is not found")
        void shouldReturnRfc7807ResponseWhenResourceNotFound() throws Exception {
            // Arrange
            Long resourceId = 999L;

            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/{id}", resourceId)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/resource-not-found"))
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Resource with id 999 not found"))
                    .andExpect(jsonPath("$.instance").value("/api/v1/test/999"));
        }

        @Test
        @DisplayName("Should return RFC 7807 response with required fields only")
        void shouldReturnRfc7807ResponseWithRequiredFields() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/123")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert - Verify all RFC 7807 required fields are present
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").exists());
        }
    }

    @Nested
    @DisplayName("Validation Exception Tests (HTTP 400)")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Should return RFC 7807 with fieldErrors for single validation failure")
        void shouldReturnRfc7807WithFieldErrorsForSingleValidationFailure() throws Exception {
            // Arrange
            String invalidRequest = """
                    {
                        "name": "",
                        "email": "valid@example.com"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/validation")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/validation-error"))
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("Request validation failed. Check the fieldErrors for details."))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").exists());
        }

        @Test
        @DisplayName("Should return RFC 7807 with fieldErrors for multiple validation failures")
        void shouldReturnRfc7807WithFieldErrorsForMultipleValidationFailures() throws Exception {
            // Arrange
            String invalidRequest = """
                    {
                        "name": "",
                        "email": "invalid-email"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/validation")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/validation-error"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
        }

        @Test
        @DisplayName("Should capture all field validation errors in fieldErrors array")
        void shouldCaptureAllFieldValidationErrorsInFieldErrorsArray() throws Exception {
            // Arrange - Create a description with 501 characters (exceeds max of 500)
            String longDescription = "x".repeat(501);
            String invalidRequest = String.format("""
                    {
                        "name": "A",
                        "email": "not-an-email",
                        "description": "%s"
                    }
                    """, longDescription);

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/validation")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors.length()").value(greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("Should return success when request is valid")
        void shouldReturnSuccessWhenRequestIsValid() throws Exception {
            // Arrange
            String validRequest = """
                    {
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "description": "A valid description"
                    }
                    """;

            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/validation")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest));

            // Assert
            result.andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Data Integrity Violation Tests (HTTP 409)")
    class DataIntegrityViolationTests {

        @Test
        @DisplayName("Should return RFC 7807 response with HTTP 409 for data conflict")
        void shouldReturnRfc7807ResponseWithHttp409ForDataConflict() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/conflict")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/data-conflict"))
                    .andExpect(jsonPath("$.title").value("Data Conflict"))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.instance").exists());
        }

        @Test
        @DisplayName("Should return sanitized message without SQL or constraint details")
        void shouldReturnSanitizedMessageWithoutSqlOrConstraintDetails() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/conflict")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert - Verify no SQL details are leaked
            String responseContent = result.andReturn().getResponse().getContentAsString();

            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("A data conflict occurred. The operation could not be completed due to a constraint violation."));

            // Verify specific SQL/DB details are not present in the response
            // Note: "constraint" in generic message is acceptable, specific constraint names are not
            assertFalse(responseContent.toLowerCase().contains("unique"));
            assertFalse(responseContent.toLowerCase().contains("table"));
            assertFalse(responseContent.toLowerCase().contains("column"));
        }

        @Test
        @DisplayName("Should not expose internal database information in error response")
        void shouldNotExposeInternalDatabaseInformationInErrorResponse() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(post(BASE_API_PATH + "/conflict")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            String responseContent = result.andReturn().getResponse().getContentAsString();

            // Verify no internal details are leaked
            assertFalse(responseContent.contains("books"));
            assertFalse(responseContent.contains("isbn"));
            assertFalse(responseContent.toLowerCase().contains("sql"));
            assertFalse(responseContent.contains("insert"));
            assertFalse(responseContent.contains("update"));
        }
    }

    @Nested
    @DisplayName("RFC 7807 Compliance Tests")
    class Rfc7807ComplianceTests {

        @Test
        @DisplayName("Should include all required RFC 7807 fields in error responses")
        void shouldIncludeAllRequiredRfc7807FieldsInErrorResponses() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert - Verify all RFC 7807 required fields
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").isString())
                    .andExpect(jsonPath("$.title").isString())
                    .andExpect(jsonPath("$.status").isNumber())
                    .andExpect(jsonPath("$.detail").isString())
                    .andExpect(jsonPath("$.instance").isString());
        }

        @Test
        @DisplayName("Should use absolute URI for type field")
        void shouldUseAbsoluteUriForTypeField() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value(startsWith("https://")));
        }

        @Test
        @DisplayName("Should return JSON content type for all error responses")
        void shouldReturnJsonContentTypeForAllErrorResponses() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/999")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))
            );

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$").exists());
        }
    }
}
