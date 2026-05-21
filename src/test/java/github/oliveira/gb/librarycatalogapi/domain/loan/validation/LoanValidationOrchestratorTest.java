package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BatchLimitExceededException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.DuplicateTitleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoanValidationOrchestrator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Validation Orchestrator Unit Tests")
class LoanValidationOrchestratorTest {

    @Test
    @DisplayName("Should execute all validators in sequence when all pass")
    void shouldExecuteAllValidatorsWhenAllPass() {
        LoanValidator v1 = mock(LoanValidator.class);
        LoanValidator v2 = mock(LoanValidator.class);
        LoanValidationOrchestrator orchestrator = new LoanValidationOrchestrator(List.of(v1, v2));
        LoanValidationContext context = new LoanValidationContext(null, List.of(1L), null, Instant.now());

        orchestrator.validate(context);

        verify(v1).validate(context);
        verify(v2).validate(context);
    }

    @Test
    @DisplayName("Should fail fast on first validator failure and not execute subsequent validators")
    void shouldFailFastOnFirstValidatorFailure() {
        LoanValidator v1 = mock(LoanValidator.class);
        LoanValidator v2 = mock(LoanValidator.class);
        LoanValidationOrchestrator orchestrator = new LoanValidationOrchestrator(List.of(v1, v2));
        LoanValidationContext context = new LoanValidationContext(null, List.of(1L, 2L, 3L, 4L, 5L, 6L), null, Instant.now());

        doThrow(new BatchLimitExceededException("too many")).when(v1).validate(context);

        assertThatThrownBy(() -> orchestrator.validate(context))
                .isInstanceOf(BatchLimitExceededException.class);

        verify(v1).validate(context);
        verify(v2, never()).validate(context);
    }

    @Test
    @DisplayName("Should stop at duplicate title validator and not reach availability validator")
    void shouldStopAtDuplicateTitleValidator() {
        LoanValidator v1 = mock(LoanValidator.class); // BatchSize - passes
        LoanValidator v2 = mock(LoanValidator.class); // DuplicateTitle - fails
        LoanValidator v3 = mock(LoanValidator.class); // BookAvailability - should not run
        LoanValidationOrchestrator orchestrator = new LoanValidationOrchestrator(List.of(v1, v2, v3));
        LoanValidationContext context = new LoanValidationContext(null, List.of(1L, 1L), null, Instant.now());

        doNothing().when(v1).validate(context);
        doThrow(new DuplicateTitleException("dup")).when(v2).validate(context);

        assertThatThrownBy(() -> orchestrator.validate(context))
                .isInstanceOf(DuplicateTitleException.class);

        verify(v1).validate(context);
        verify(v2).validate(context);
        verify(v3, never()).validate(context);
    }
}
