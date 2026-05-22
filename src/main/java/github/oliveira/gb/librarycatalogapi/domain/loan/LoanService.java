package github.oliveira.gb.librarycatalogapi.domain.loan;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.DuplicateTitleException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.LoanAlreadyReturnedException;
import github.oliveira.gb.librarycatalogapi.domain.loan.exception.MaxRenewalsReachedException;
import github.oliveira.gb.librarycatalogapi.domain.loan.validation.LoanValidationContext;
import github.oliveira.gb.librarycatalogapi.domain.loan.validation.LoanValidationOrchestrator;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import github.oliveira.gb.librarycatalogapi.domain.reader.ReaderRepository;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Core transactional service for processing batch book loans.
 * Enforces strict business rules via the validation orchestrator before persisting.
 * All operations are wrapped in a Spring {@code @Transactional} context to guarantee ACID properties.
 */
@Service
public class LoanService {

    private static final int DEFAULT_LOAN_DAYS = 14;
    private static final int MAX_RENEWALS = 3;

    private final ReaderRepository readerRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final LoanValidationOrchestrator validationOrchestrator;

    public LoanService(ReaderRepository readerRepository,
                       BookRepository bookRepository,
                       LoanRepository loanRepository,
                       LoanValidationOrchestrator validationOrchestrator) {
        this.readerRepository = readerRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.validationOrchestrator = validationOrchestrator;
    }

    /**
     * Processes a batch loan request atomically.
     *
     * <p>Execution flow:</p>
     * <ol>
     *   <li>Lock the reader record pessimistically to serialize concurrent requests.</li>
     *   <li>Lock the requested book records (ordered by ID to prevent deadlocks).</li>
     *   <li>Validate that all requested books exist.</li>
     *   <li>Run the validation orchestrator (fail-fast, no entity mutation yet).</li>
     *   <li>Mutate book statuses to EMPRESTADO and create Loan + LoanItems.</li>
     *   <li>Persist the loan transaction.</li>
     * </ol>
     *
     * @param readerId the reader requesting the loan
     * @param bookIds  the list of book IDs to loan
     * @return the persisted Loan entity
     * @throws ResourceNotFoundException if the reader or any book is not found
     * @throws RuntimeException          if any validation rule is violated
     */
    @Transactional
    public Loan createLoan(Long readerId, List<Long> bookIds) {
        Reader reader = readerRepository.findWithLockById(readerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reader not found with id: " + readerId));

        long distinctCount = bookIds.stream().distinct().count();
        if (distinctCount != bookIds.size()) {
            throw new DuplicateTitleException(
                    "The loan batch contains duplicate titles. Each title can only be requested once per batch."
            );
        }

        List<Long> sortedBookIds = bookIds.stream().sorted().toList();
        List<Book> books = bookRepository.findAllByIdWithLock(sortedBookIds);

        if (books.size() != sortedBookIds.size()) {
            throw new ResourceNotFoundException("One or more requested books were not found.");
        }

        Instant now = Instant.now();
        LoanValidationContext context = new LoanValidationContext(reader, sortedBookIds, books, now);
        validationOrchestrator.validate(context);

        Loan loan = new Loan();
        loan.setReader(reader);
        loan.setStatus(LoanStatus.ATIVO);
        loan.setDueDate(now.plus(DEFAULT_LOAN_DAYS, ChronoUnit.DAYS));

        for (Book book : books) {
            book.setStatus(BookStatus.EMPRESTADO);
            LoanItem item = new LoanItem();
            item.setLoan(loan);
            item.setBook(book);
            loan.getItems().add(item);
        }

        Loan saved = loanRepository.save(loan);

        // Force initialization of lazy proxies before leaving the transactional boundary.
        // This ensures the controller can safely map the entity to a DTO without
        // triggering a LazyInitializationException.
        saved.getItems().forEach(item -> item.getBook().getId());

        return saved;
    }

    /**
     * Renews an active loan by extending its due date and incrementing the renewal counter.
     *
     * <p>Business rules enforced:</p>
     * <ul>
     *   <li>The loan must have status {@code ATIVO}.</li>
     *   <li>The loan must not have reached the maximum allowed renewals ({@value MAX_RENEWALS}).</li>
     * </ul>
     *
     * @param loanId the loan identifier to renew
     * @return the updated Loan entity
     * @throws ResourceNotFoundException      if the loan does not exist
     * @throws LoanAlreadyReturnedException   if the loan has already been finalized
     * @throws MaxRenewalsReachedException    if the maximum renewal count has been reached
     */
    @Transactional
    public Loan renewLoan(Long loanId) {
        Loan loan = loanRepository.findWithLockById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.ATIVO) {
            throw new LoanAlreadyReturnedException(
                    "Loan has already been returned and cannot be renewed."
            );
        }

        if (loan.getRenewalCount() >= MAX_RENEWALS) {
            throw new MaxRenewalsReachedException(
                    "Maximum number of renewals (" + MAX_RENEWALS + ") has already been reached for this loan."
            );
        }

        Instant now = Instant.now();
        loan.setDueDate(now.plus(DEFAULT_LOAN_DAYS, ChronoUnit.DAYS));
        loan.setRenewalCount(loan.getRenewalCount() + 1);

        return loan;
    }
}
