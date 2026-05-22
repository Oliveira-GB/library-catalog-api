package github.oliveira.gb.librarycatalogapi.api.loan;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.Loan;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanItem;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanRepository;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanStatus;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import github.oliveira.gb.librarycatalogapi.domain.reader.ReaderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for loan renewal endpoint.
 * Validates term extension, maximum renewal enforcement, and state immutability
 * on rejection using Testcontainers with a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Loan Renewal Integration Tests")
class LoanRenewalIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
            .withDatabaseName("catalog_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LoanRepository loanRepository;

    private void cleanAll() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        categoryRepository.deleteAll();
        readerRepository.deleteAll();
    }

    private Reader createTestReader(String email, String cpf) {
        Reader reader = new Reader();
        reader.setName("Test Reader");
        reader.setEmail(email);
        reader.setCpf(cpf);
        return readerRepository.save(reader);
    }

    private Category createTestCategory() {
        Category category = new Category();
        category.setName("Test Category " + System.nanoTime());
        return categoryRepository.save(category);
    }

    private Book createTestBook(String isbn, String title, Category category) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setCategory(category);
        return bookRepository.save(book);
    }

    private Loan createLoanWithRenewals(Reader reader, Book book, int renewalCount, LoanStatus status, Instant dueDate) {
        Loan loan = new Loan();
        loan.setReader(reader);
        loan.setStatus(status);
        loan.setDueDate(dueDate);
        loan.setRenewalCount(renewalCount);
        Loan savedLoan = loanRepository.save(loan);

        LoanItem item = new LoanItem();
        item.setLoan(savedLoan);
        item.setBook(book);
        savedLoan.getItems().add(item);
        return loanRepository.save(savedLoan);
    }

    @Test
    @DisplayName("Should renew loan successfully and update due date")
    void shouldRenewLoanSuccessfully() throws Exception {
        try {
            Reader reader = createTestReader("renew@example.com", "123.456.789-10");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000400-0", "Renew Book", category);
            Instant originalDueDate = Instant.now().minus(1, ChronoUnit.DAYS);
            Loan loan = createLoanWithRenewals(reader, book, 2, LoanStatus.ATIVO, originalDueDate);

            ResultActions result = mockMvc.perform(patch("/api/v1/emprestimos/{id}/renovacao", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(loan.getId().intValue()))
                    .andExpect(jsonPath("$.status").value("ATIVO"))
                    .andExpect(jsonPath("$.renewalCount").value(3));

            // Assert database state
            Loan persistedLoan = loanRepository.findById(loan.getId()).orElseThrow();
            assertThat(persistedLoan.getRenewalCount()).isEqualTo(3);
            assertThat(persistedLoan.getDueDate()).isAfter(originalDueDate);
        } finally {
            cleanAll();
        }
    }

    @Test
    @DisplayName("Should return HTTP 422 when max renewals reached and leave state unchanged")
    void shouldReturnHttp422WhenMaxRenewalsReached() throws Exception {
        try {
            Reader reader = createTestReader("maxrenew@example.com", "123.456.789-11");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000401-0", "Max Renew Book", category);
            Instant originalDueDate = Instant.now().plus(5, ChronoUnit.DAYS);
            Loan loan = createLoanWithRenewals(reader, book, 3, LoanStatus.ATIVO, originalDueDate);

            ResultActions result = mockMvc.perform(patch("/api/v1/emprestimos/{id}/renovacao", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Loan Validation Failed"));

            // Assert state is unchanged
            Loan persistedLoan = loanRepository.findById(loan.getId()).orElseThrow();
            assertThat(persistedLoan.getRenewalCount()).isEqualTo(3);
            assertThat(persistedLoan.getDueDate()).isCloseTo(originalDueDate, within(1, ChronoUnit.MILLIS));
        } finally {
            cleanAll();
        }
    }

    @Test
    @DisplayName("Should return HTTP 404 when loan does not exist")
    void shouldReturnHttp404WhenLoanNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/emprestimos/{id}/renovacao", 99999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("Should return HTTP 422 when loan is already finalized and leave state unchanged")
    void shouldReturnHttp422WhenLoanAlreadyFinalized() throws Exception {
        try {
            Reader reader = createTestReader("finalized@example.com", "123.456.789-12");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000402-0", "Finalized Book", category);
            Instant originalDueDate = Instant.now().plus(3, ChronoUnit.DAYS);
            Loan loan = createLoanWithRenewals(reader, book, 1, LoanStatus.FINALIZADO, originalDueDate);

            ResultActions result = mockMvc.perform(patch("/api/v1/emprestimos/{id}/renovacao", loan.getId())
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Loan Validation Failed"));

            // Assert state is unchanged
            Loan persistedLoan = loanRepository.findById(loan.getId()).orElseThrow();
            assertThat(persistedLoan.getRenewalCount()).isEqualTo(1);
            assertThat(persistedLoan.getDueDate()).isCloseTo(originalDueDate, within(1, ChronoUnit.MILLIS));
            assertThat(persistedLoan.getStatus()).isEqualTo(LoanStatus.FINALIZADO);
        } finally {
            cleanAll();
        }
    }

    @Test
    @DisplayName("Should serialize concurrent renewal requests for the same loan")
    void shouldSerializeConcurrentRenewalRequests() throws Exception {
        try {
            Reader reader = createTestReader("renewal-concurrent@example.com", "123.456.789-13");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000403-0", "Concurrent Renew Book", category);
            Instant originalDueDate = Instant.now().plus(7, ChronoUnit.DAYS);
            Loan loan = createLoanWithRenewals(reader, book, 2, LoanStatus.ATIVO, originalDueDate);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    latch.await();
                    var response = mockMvc.perform(patch("/api/v1/emprestimos/{id}/renovacao", loan.getId())
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andReturn()
                            .getResponse();
                    return response.getStatus();
                }));
            }

            latch.countDown();

            int status1 = futures.get(0).get();
            int status2 = futures.get(1).get();
            executor.shutdown();

            List<Integer> statuses = List.of(status1, status2);
            assertThat(statuses).containsExactlyInAnyOrder(200, 422);

            // Assert final state: exactly 3 renewals, not 4
            Loan persistedLoan = loanRepository.findById(loan.getId()).orElseThrow();
            assertThat(persistedLoan.getRenewalCount()).isEqualTo(3);
        } finally {
            cleanAll();
        }
    }
}
