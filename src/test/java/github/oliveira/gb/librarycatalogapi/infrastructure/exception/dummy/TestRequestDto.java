package github.oliveira.gb.librarycatalogapi.infrastructure.exception.dummy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Test DTO for validation exception handling tests.
 * Used exclusively in test scope to trigger MethodArgumentNotValidException.
 */
public record TestRequestDto(

        @NotBlank(message = "Name must not be blank")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a well-formed email address")
        String email,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
