package github.oliveira.gb.librarycatalogapi.domain.loan;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.BookUnavailableException;
import github.oliveira.gb.librarycatalogapi.domain.loan.validation.LoanValidationContext;
import github.oliveira.gb.librarycatalogapi.domain.loan.validation.LoanValidationOrchestrator;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import github.oliveira.gb.librarycatalogapi.domain.reader.ReaderRepository;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoanService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Service Unit Tests")
class LoanServiceTest {

    @Mock
    private ReaderRepository readerRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanValidationOrchestrator orchestrator;

    @InjectMocks
    private LoanService loanService;

    @Test
    @DisplayName("Should create loan successfully when all validations pass")
    void shouldCreateLoanSuccessfully() {
        // Arrange
        Long readerId = 1L;
        List<Long> bookIds = List.of(10L, 20L);

        Reader reader = new Reader();
        reader.setId(readerId);

        Book book1 = new Book();
        book1.setId(10L);
        book1.setStatus(BookStatus.DISPONIVEL);
        Book book2 = new Book();
        book2.setId(20L);
        book2.setStatus(BookStatus.DISPONIVEL);

        when(readerRepository.findWithLockById(readerId)).thenReturn(Optional.of(reader));
        when(bookRepository.findAllByIdWithLock(List.of(10L, 20L))).thenReturn(List.of(book1, book2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan loan = inv.getArgument(0);
            loan.setId(100L);
            return loan;
        });

        // Act
        Loan result = loanService.createLoan(readerId, bookIds);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getReader().getId()).isEqualTo(readerId);
        assertThat(result.getStatus()).isEqualTo(LoanStatus.ATIVO);
        assertThat(result.getItems()).hasSize(2);
        assertThat(book1.getStatus()).isEqualTo(BookStatus.EMPRESTADO);
        assertThat(book2.getStatus()).isEqualTo(BookStatus.EMPRESTADO);

        verify(readerRepository).findWithLockById(readerId);
        verify(bookRepository).findAllByIdWithLock(List.of(10L, 20L));
        verify(orchestrator).validate(any(LoanValidationContext.class));
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when reader not found")
    void shouldThrowWhenReaderNotFound() {
        when(readerRepository.findWithLockById(1L)).thenReturn(Optional.empty());
        List<Long> bookIds = List.of(10L);

        assertThatThrownBy(() -> loanService.createLoan(1L, bookIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reader not found");

        verify(bookRepository, never()).findAllByIdWithLock(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when a book is not found")
    void shouldThrowWhenBookNotFound() {
        Reader reader = new Reader();
        reader.setId(1L);
        when(readerRepository.findWithLockById(1L)).thenReturn(Optional.of(reader));
        List<Long> bookIds = List.of(10L, 20L);
        when(bookRepository.findAllByIdWithLock(bookIds)).thenReturn(List.of(new Book())); // only 1 returned

        assertThatThrownBy(() -> loanService.createLoan(1L, bookIds))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("One or more requested books were not found");

        verify(orchestrator, never()).validate(any());
    }

    @Test
    @DisplayName("Should throw when orchestrator validation fails")
    void shouldThrowWhenValidationFails() {
        Reader reader = new Reader();
        reader.setId(1L);
        Book book = new Book();
        book.setId(10L);
        book.setStatus(BookStatus.DISPONIVEL);

        when(readerRepository.findWithLockById(1L)).thenReturn(Optional.of(reader));
        List<Long> bookIds = List.of(10L);
        when(bookRepository.findAllByIdWithLock(bookIds)).thenReturn(List.of(book));
        doThrow(new BookUnavailableException("not available"))
                .when(orchestrator).validate(any(LoanValidationContext.class));

        assertThatThrownBy(() -> loanService.createLoan(1L, bookIds))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessageContaining("not available");

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should sort bookIds before querying to prevent deadlocks")
    void shouldSortBookIdsBeforeLocking() {
        Reader reader = new Reader();
        reader.setId(1L);
        Book book1 = new Book();
        book1.setId(10L);
        Book book2 = new Book();
        book2.setId(20L);

        when(readerRepository.findWithLockById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findAllByIdWithLock(List.of(10L, 20L))).thenReturn(List.of(book1, book2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        loanService.createLoan(1L, List.of(20L, 10L)); // unsorted input

        verify(bookRepository).findAllByIdWithLock(List.of(10L, 20L));
    }
}
