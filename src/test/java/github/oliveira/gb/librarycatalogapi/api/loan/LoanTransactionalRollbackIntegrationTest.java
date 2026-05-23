package github.oliveira.gb.librarycatalogapi.api.loan;

import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import github.oliveira.gb.librarycatalogapi.domain.fine.Fine;
import github.oliveira.gb.librarycatalogapi.domain.fine.FineRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests dedicated to verifying {@code @Transactional} rollback behavior.
 * Ensures that when a loan validation fails, no entity mutation occurs and the database
 * remains in a pristine state.
 *
 * <p><strong>Important:</strong> This class does NOT use {@code @AfterEach} automatic cleanup.
 * Each test is responsible for its own data setup and manual cleanup to allow
 * post-failure database state assertions.</p>
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Loan Transactional Rollback Integration Tests")
class LoanTransactionalRollbackIntegrationTest {

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

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASS = "admin123";

    private static final String BASE_API_PATH = "/api/v1/emprestimos";

    private void cleanAll() {
        // Native SQL DELETE bypasses Hibernate @SQLRestriction filters,
        // ensuring soft-deleted (inactive) records are also removed.
        jdbcTemplate.execute("DELETE FROM loan_items");
        jdbcTemplate.execute("DELETE FROM loans");
        jdbcTemplate.execute("DELETE FROM fines");
        jdbcTemplate.execute("DELETE FROM book_authors");
        jdbcTemplate.execute("DELETE FROM books");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM readers");
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

    private Loan createActiveLoan(Reader reader, List<Book> books, Instant dueDate) {
        Loan loan = new Loan();
        loan.setReader(reader);
        loan.setStatus(LoanStatus.ATIVO);
        loan.setDueDate(dueDate);
        Loan savedLoan = loanRepository.save(loan);

        for (Book book : books) {
            book.setStatus(BookStatus.EMPRESTADO);
            bookRepository.save(book);

            LoanItem item = new LoanItem();
            item.setLoan(savedLoan);
            item.setBook(book);
            savedLoan.getItems().add(item);
        }
        return loanRepository.save(savedLoan);
    }

    // ------------------------------------------------------------------------
    // 1. Active Fine -> Rollback
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when reader has active unpaid fine")
    void shouldRollbackWhenReaderHasActiveFine() throws Exception {
        try {
            Reader reader = createTestReader("fine@example.com", "123.456.789-00");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000001-0", "Fine Book", category);

            Fine fine = new Fine();
            fine.setReader(reader);
            fine.setAmount(new BigDecimal("50.00"));
            fine.setPaid(false);
            fineRepository.save(fine);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), book.getId());

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableEntity());

            // Assert database state is pristine
            Book persistedBook = bookRepository.findById(book.getId()).orElseThrow();
            assertThat(persistedBook.getStatus()).isEqualTo(BookStatus.DISPONIVEL);
            assertThat(loanRepository.count()).isZero();
        } finally {
            cleanAll();
        }
    }

    // ------------------------------------------------------------------------
    // 2. Overdue Book -> Rollback
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when reader has an overdue book")
    void shouldRollbackWhenReaderHasOverdueBook() throws Exception {
        try {
            Reader reader = createTestReader("overdue@example.com", "123.456.789-01");
            Category category = createTestCategory();
            Book overdueBook = createTestBook("978-0-00-000002-0", "Overdue Book", category);
            Book newBook = createTestBook("978-0-00-000003-0", "New Book", category);

            // Create an active overdue loan for the reader
            Instant pastDueDate = Instant.now().minus(1, ChronoUnit.DAYS);
            createActiveLoan(reader, List.of(overdueBook), pastDueDate);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), newBook.getId());

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableEntity());

            // Assert the newly requested book was NOT mutated
            Book persistedNewBook = bookRepository.findById(newBook.getId()).orElseThrow();
            assertThat(persistedNewBook.getStatus()).isEqualTo(BookStatus.DISPONIVEL);
            assertThat(loanRepository.count()).isEqualTo(1); // only the original overdue loan exists
        } finally {
            cleanAll();
        }
    }

    // ------------------------------------------------------------------------
    // 3. Batch >= 6 -> Rollback
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when batch contains exactly 6 books")
    void shouldRollbackWhenBatchContainsSixBooks() throws Exception {
        try {
            Reader reader = createTestReader("batch@example.com", "123.456.789-02");
            Category category = createTestCategory();
            List<Book> books = new java.util.ArrayList<>();
            for (int i = 0; i < 6; i++) {
                books.add(createTestBook("978-0-00-00000" + i + "-0", "Book " + i, category));
            }

            StringBuilder bookIds = new StringBuilder();
            for (int i = 0; i < books.size(); i++) {
                if (i > 0) bookIds.append(", ");
                bookIds.append(books.get(i).getId());
            }

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%s]
                    }
                    """, reader.getId(), bookIds);

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableEntity());

            // Assert none of the books were mutated
            for (Book book : books) {
                Book persisted = bookRepository.findById(book.getId()).orElseThrow();
                assertThat(persisted.getStatus()).isEqualTo(BookStatus.DISPONIVEL);
            }
            assertThat(loanRepository.count()).isZero();
        } finally {
            cleanAll();
        }
    }

    // ------------------------------------------------------------------------
    // 4. Cumulative Possession Limit (4 active + 2 requested = 6) -> Rollback
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when cumulative possession limit exceeds 5")
    void shouldRollbackWhenCumulativeLimitExceeded() throws Exception {
        try {
            Reader reader = createTestReader("cumulative@example.com", "123.456.789-03");
            Category category = createTestCategory();

            // Create 4 active loans for the reader
            List<Book> activeBooks = new java.util.ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Book b = createTestBook("978-0-00-00010" + i + "-0", "Active Book " + i, category);
                activeBooks.add(b);
            }
            Instant futureDueDate = Instant.now().plus(7, ChronoUnit.DAYS);
            createActiveLoan(reader, activeBooks, futureDueDate);

            // Request 2 more books
            Book newBook1 = createTestBook("978-0-00-000200-0", "New Book 1", category);
            Book newBook2 = createTestBook("978-0-00-000201-0", "New Book 2", category);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d, %d]
                    }
                    """, reader.getId(), newBook1.getId(), newBook2.getId());

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableEntity());

            // Assert the 2 newly requested books were NOT mutated
            assertThat(bookRepository.findById(newBook1.getId()).orElseThrow().getStatus()).isEqualTo(BookStatus.DISPONIVEL);
            assertThat(bookRepository.findById(newBook2.getId()).orElseThrow().getStatus()).isEqualTo(BookStatus.DISPONIVEL);

            // Only the original active loan should exist
            assertThat(loanRepository.count()).isEqualTo(1);
        } finally {
            cleanAll();
        }
    }

    // ------------------------------------------------------------------------
    // 5. Duplicate Title in Batch -> Rollback
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when batch contains duplicate titles")
    void shouldRollbackWhenDuplicateTitlesInBatch() throws Exception {
        try {
            Reader reader = createTestReader("dup@example.com", "123.456.789-04");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000300-0", "Dup Book", category);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d, %d]
                    }
                    """, reader.getId(), book.getId(), book.getId());

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnprocessableEntity());

            // Assert the book was NOT mutated
            Book persistedBook = bookRepository.findById(book.getId()).orElseThrow();
            assertThat(persistedBook.getStatus()).isEqualTo(BookStatus.DISPONIVEL);
            assertThat(loanRepository.count()).isZero();
        } finally {
            cleanAll();
        }
    }

    // ------------------------------------------------------------------------
    // 6. Soft-Deleted Book -> 404, no mutation
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when book is soft-deleted (active=false)")
    void shouldRollbackWhenBookIsSoftDeleted() throws Exception {
        try {
            Reader reader = createTestReader("inactive-book@example.com", "123.456.789-97");
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000097-0", "Inactive Book", category);
            book.setActive(false);
            bookRepository.save(book);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), book.getId());

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));

            assertThat(loanRepository.count()).isZero();
        } finally {
            cleanAll();
        }
    }

    // ------------------------------------------------------------------------
    // 7. Soft-Deleted Reader -> 404, no mutation
    // ------------------------------------------------------------------------
    @Test
    @DisplayName("Should rollback when reader is soft-deleted (active=false)")
    void shouldRollbackWhenReaderIsSoftDeleted() throws Exception {
        try {
            Reader reader = createTestReader("inactive-reader@example.com", "123.456.789-96");
            reader.setActive(false);
            readerRepository.save(reader);
            Category category = createTestCategory();
            Book book = createTestBook("978-0-00-000096-0", "Reader Inactive Book", category);

            String requestBody = String.format("""
                    {
                        "readerId": %d,
                        "bookIds": [%d]
                    }
                    """, reader.getId(), book.getId());

            mockMvc.perform(post(BASE_API_PATH)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS))

                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));

            assertThat(loanRepository.count()).isZero();
        } finally {
            cleanAll();
        }
    }
}
