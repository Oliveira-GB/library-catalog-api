package github.oliveira.gb.librarycatalogapi.infrastructure.exception.dummy;

import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Dummy controller for testing global exception handling.
 * This controller is strictly for test purposes and resides in the test source set.
 * It provides endpoints to trigger various exception scenarios for validation.
 */
@RestController
@RequestMapping("/api/v1/test")
public class DummyController {

    /**
     * Triggers a ResourceNotFoundException to test HTTP 404 handling.
     *
     * @param id the resource identifier
     * @return never returns successfully - always throws exception
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getResource(@PathVariable Long id) {
        throw new ResourceNotFoundException("Resource with id " + id + " not found");
    }

    /**
     * Triggers a MethodArgumentNotValidException to test HTTP 400 handling.
     *
     * @param requestDto the request body with validation constraints
     * @return success message if validation passes
     */
    @PostMapping("/validation")
    public ResponseEntity<String> createWithValidation(@Valid @RequestBody TestRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body("Created successfully");
    }

    /**
     * Triggers a DataIntegrityViolationException to test HTTP 409 handling.
     *
     * @return never returns successfully - always throws exception
     */
    @PostMapping("/conflict")
    public ResponseEntity<String> createWithConflict() {
        throw new DataIntegrityViolationException("Unique constraint violation on table books column isbn");
    }

    /**
     * Triggers a generic RuntimeException to test HTTP 500 fallback handling.
     *
     * @return never returns successfully - always throws exception
     */
    @GetMapping("/server-error")
    public ResponseEntity<String> triggerServerError() {
        throw new RuntimeException("Critical internal failure: database connection pool exhausted");
    }
}
