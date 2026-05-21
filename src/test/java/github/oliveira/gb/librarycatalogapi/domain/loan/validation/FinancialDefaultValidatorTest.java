package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import github.oliveira.gb.librarycatalogapi.domain.fine.FineRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.PendingFinesException;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FinancialDefaultValidator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Financial Default Validator Unit Tests")
class FinancialDefaultValidatorTest {

    @Mock
    private FineRepository fineRepository;

    @InjectMocks
    private FinancialDefaultValidator validator;

    @Test
    @DisplayName("Should pass when reader has no unpaid fines")
    void shouldPassWhenNoUnpaidFines() {
        Reader reader = new Reader();
        reader.setId(1L);
        when(fineRepository.existsUnpaidFineByReaderId(1L)).thenReturn(false);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L), null, Instant.now());
        assertThatNoException().isThrownBy(() -> validator.validate(context));
        verify(fineRepository).existsUnpaidFineByReaderId(1L);
    }

    @Test
    @DisplayName("Should throw when reader has unpaid fines")
    void shouldThrowWhenUnpaidFinesExist() {
        Reader reader = new Reader();
        reader.setId(2L);
        when(fineRepository.existsUnpaidFineByReaderId(2L)).thenReturn(true);

        LoanValidationContext context = new LoanValidationContext(reader, List.of(10L), null, Instant.now());
        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(PendingFinesException.class)
                .hasMessageContaining("unpaid fines");
    }
}
