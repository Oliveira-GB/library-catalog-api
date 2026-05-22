package github.oliveira.gb.librarycatalogapi.api.report;

import github.oliveira.gb.librarycatalogapi.domain.author.Author;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.Book;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import github.oliveira.gb.librarycatalogapi.domain.fine.Fine;
import github.oliveira.gb.librarycatalogapi.domain.fine.FineRepository;
import github.oliveira.gb.librarycatalogapi.domain.reader.Reader;
import github.oliveira.gb.librarycatalogapi.domain.reader.ReaderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReportController.
 * Validates content negotiation, authentication requirements, 100-record limit,
 * soft-delete bypass in financial report, and empty result handling.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Report Controller Integration Tests")
class ReportControllerIntegrationTest {

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
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE_API_PATH = "/api/v1/relatorios";
    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASS = "admin";

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM fines");
        jdbcTemplate.execute("DELETE FROM loan_items");
        jdbcTemplate.execute("DELETE FROM loans");
        jdbcTemplate.execute("DELETE FROM book_authors");
        jdbcTemplate.execute("DELETE FROM books");
        jdbcTemplate.execute("DELETE FROM authors");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM readers");
    }

    @Nested
    @DisplayName("GET /api/v1/relatorios/inventario - Inventory Report")
    class InventoryReportTests {

        @Test
        @DisplayName("Should return JSON inventory report when authenticated")
        void shouldReturnJsonInventoryReportWhenAuthenticated() throws Exception {
            Category cat = createCategory("Fiction");
            Author auth = createAuthor("Author", "a@example.com");
            createBook("Book A", "978-0-00-000001-0", cat, auth, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].bookId").isNumber())
                    .andExpect(jsonPath("$[0].title").value("Book A"))
                    .andExpect(jsonPath("$[0].isbn").value("978-0-00-000001-0"))
                    .andExpect(jsonPath("$[0].status").value("DISPONIVEL"));
        }

        @Test
        @DisplayName("Should return CSV inventory report when authenticated")
        void shouldReturnCsvInventoryReportWhenAuthenticated() throws Exception {
            Category cat = createCategory("Tech");
            Author auth = createAuthor("Dev", "dev@example.com");
            createBook("Dev Book", "978-0-00-000002-0", cat, auth, BookStatus.EMPRESTADO);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", "text/csv")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"))
                    .andExpect(content().string(containsString("bookId,title,isbn,status")))
                    .andExpect(content().string(containsString("Dev Book")))
                    .andExpect(content().string(containsString("EMPRESTADO")));
        }

        @Test
        @DisplayName("Should return PDF inventory report when authenticated")
        void shouldReturnPdfInventoryReportWhenAuthenticated() throws Exception {
            Category cat = createCategory("History");
            Author auth = createAuthor("Hist", "hist@example.com");
            createBook("History Book", "978-0-00-000003-0", cat, auth, BookStatus.DISPONIVEL);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", MediaType.APPLICATION_PDF_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE));

            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 406 for unsupported Accept header")
        void shouldReturn406ForUnsupportedAcceptHeader() throws Exception {
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", "image/png")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isNotAcceptable())
                    .andExpect(jsonPath("$.type").value("https://api.library-catalog.com/errors/media-type-not-acceptable"));
        }

        @Test
        @DisplayName("Should exclude inactive books from inventory report")
        void shouldExcludeInactiveBooksFromInventoryReport() throws Exception {
            Category cat = createCategory("InactiveTest");
            Author auth = createAuthor("InactiveAuthor", "inactive@example.com");
            Book activeBook = createBook("Active Book", "978-0-00-000004-0", cat, auth, BookStatus.DISPONIVEL);
            Book inactiveBook = createBook("Inactive Book", "978-0-00-000005-0", cat, auth, BookStatus.DISPONIVEL);
            inactiveBook.setActive(false);
            bookRepository.save(inactiveBook);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Active Book"));
        }

        @Test
        @DisplayName("Should enforce maximum of 100 records in inventory report")
        void shouldEnforceMaximumOf100RecordsInInventoryReport() throws Exception {
            Category cat = createCategory("BulkInventory");
            Author auth = createAuthor("Bulk", "bulk@example.com");
            for (int i = 0; i < 101; i++) {
                createBook("Inv Book " + i, "978-0-00-000" + String.format("%03d", i) + "-0", cat, auth, BookStatus.DISPONIVEL);
            }

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/inventario")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/relatorios/financeiro - Financial Report")
    class FinancialReportTests {

        @Test
        @DisplayName("Should return JSON financial report when authenticated")
        void shouldReturnJsonFinancialReportWhenAuthenticated() throws Exception {
            Reader reader = createReader("Finance Reader", "fin@example.com", "123.456.789-00");
            createFine(reader, new BigDecimal("25.00"), false);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].readerName").value("Finance Reader"))
                    .andExpect(jsonPath("$[0].amount").value(25.00))
                    .andExpect(jsonPath("$[0].status").value("PENDENTE"));
        }

        @Test
        @DisplayName("Should include fines from inactive readers in financial report")
        void shouldIncludeFinesFromInactiveReaders() throws Exception {
            Reader reader = createReader("Inactive Reader", "inactive@example.com", "123.456.789-01");
            createFine(reader, new BigDecimal("50.00"), true);

            jdbcTemplate.update("UPDATE readers SET active = false WHERE id = ?", reader.getId());

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].readerName").value("Inactive Reader"))
                    .andExpect(jsonPath("$[0].status").value("PAGA"));
        }

        @Test
        @DisplayName("Should return CSV financial report when authenticated")
        void shouldReturnCsvFinancialReportWhenAuthenticated() throws Exception {
            Reader reader = createReader("Csv Reader", "csv@example.com", "123.456.789-02");
            createFine(reader, new BigDecimal("10.00"), false);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", "text/csv")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"))
                    .andExpect(content().string(containsString("fineId,readerId,readerName,amount,status,createdAt")))
                    .andExpect(content().string(containsString("Csv Reader")))
                    .andExpect(content().string(containsString("PENDENTE")));
        }

        @Test
        @DisplayName("Should return PDF financial report when authenticated")
        void shouldReturnPdfFinancialReportWhenAuthenticated() throws Exception {
            Reader reader = createReader("Pdf Reader", "pdf@example.com", "123.456.789-03");
            createFine(reader, new BigDecimal("15.00"), true);

            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", MediaType.APPLICATION_PDF_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));

            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            ResultActions result = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE));

            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return empty structured documents for empty result set")
        void shouldReturnEmptyStructuredDocumentsForEmptyResultSet() throws Exception {
            ResultActions jsonResult = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));
            jsonResult.andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            ResultActions csvResult = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", "text/csv")
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));
            csvResult.andExpect(status().isOk())
                    .andExpect(content().string(containsString("fineId,readerId,readerName,amount,status,createdAt")));

            ResultActions pdfResult = mockMvc.perform(get(BASE_API_PATH + "/financeiro")
                    .header("Accept", MediaType.APPLICATION_PDF_VALUE)
                    .with(SecurityMockMvcRequestPostProcessors.httpBasic(AUTH_USER, AUTH_PASS)));
            pdfResult.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private Author createAuthor(String name, String email) {
        Author author = new Author();
        author.setName(name);
        author.setEmail(email);
        return authorRepository.save(author);
    }

    private Book createBook(String title, String isbn, Category category, Author author, BookStatus status) {
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setCategory(category);
        book.getAuthors().add(author);
        book.setStatus(status);
        book.setActive(true);
        return bookRepository.save(book);
    }

    private Reader createReader(String name, String email, String cpf) {
        Reader reader = new Reader();
        reader.setName(name);
        reader.setEmail(email);
        reader.setCpf(cpf);
        return readerRepository.save(reader);
    }

    private Fine createFine(Reader reader, BigDecimal amount, boolean paid) {
        Fine fine = new Fine();
        fine.setReader(reader);
        fine.setAmount(amount);
        fine.setPaid(paid);
        return fineRepository.save(fine);
    }
}
