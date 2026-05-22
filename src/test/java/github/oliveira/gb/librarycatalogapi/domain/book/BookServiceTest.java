package github.oliveira.gb.librarycatalogapi.domain.book;

import github.oliveira.gb.librarycatalogapi.domain.author.Author;
import github.oliveira.gb.librarycatalogapi.domain.author.AuthorRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.exception.BookLoanedException;
import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryRepository;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService.
 * Tests business logic including dependency validation, ISBN uniqueness, and inactivation rules.
 * Following TDD approach as required by project standards.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Book Service Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    @Nested
    @DisplayName("Create Book Tests")
    class CreateBookTests {

        @Test
        @DisplayName("Should create book with active status and DISPONIVEL status")
        void shouldCreateBookWithActiveStatusAndDisponivelStatus() {
            // Arrange
            String title = "Effective Java";
            String isbn = "978-0134685991";
            Long categoryId = 1L;
            List<Long> authorIds = List.of(1L);

            Category category = new Category();
            category.setId(categoryId);
            category.setName("Programming");

            Author author = new Author();
            author.setId(1L);
            author.setName("Joshua Bloch");

            Book savedBook = createBookEntity(1L, title, isbn, category, Set.of(author));

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(authorRepository.findAllById(any())).thenReturn(List.of(author));
            when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

            // Act
            Book result = bookService.create(title, isbn, categoryId, authorIds);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo(title);
            assertThat(result.getIsbn()).isEqualTo(isbn);
            assertThat(result.getStatus()).isEqualTo(BookStatus.DISPONIVEL);
            assertThat(result.getActive()).isTrue();
            assertThat(result.getCategory().getId()).isEqualTo(categoryId);
            assertThat(result.getAuthors()).hasSize(1);
            verify(bookRepository).save(any(Book.class));
            verify(authorRepository).findAllById(any());
        }

        @Test
        @DisplayName("Should trim title and ISBN before saving")
        void shouldTrimTitleAndIsbnBeforeSaving() {
            // Arrange
            String titleWithWhitespace = "  Effective Java  ";
            String isbnWithWhitespace = "  978-0134685991  ";
            Long categoryId = 1L;
            List<Long> authorIds = List.of(1L);

            Category category = new Category();
            category.setId(categoryId);

            Author author = new Author();
            author.setId(1L);

            Book savedBook = createBookEntity(1L, "Effective Java", "978-0134685991", category, Set.of(author));

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(authorRepository.findAllById(any())).thenReturn(List.of(author));
            when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

            // Act
            Book result = bookService.create(titleWithWhitespace, isbnWithWhitespace, categoryId, authorIds);

            // Assert
            assertThat(result.getTitle()).isEqualTo("Effective Java");
            assertThat(result.getIsbn()).isEqualTo("978-0134685991");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category not found")
        void shouldThrowResourceNotFoundExceptionWhenCategoryNotFound() {
            // Arrange
            Long nonExistentCategoryId = 999L;
            when(categoryRepository.findById(nonExistentCategoryId)).thenReturn(Optional.empty());

            // Act & Assert
            List<Long> authorIds = List.of(1L);
            assertThatThrownBy(() -> bookService.create("Title", "978-0-123", nonExistentCategoryId, authorIds))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found with id: " + nonExistentCategoryId);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when author not found")
        void shouldThrowResourceNotFoundExceptionWhenAuthorNotFound() {
            // Arrange
            Long categoryId = 1L;
            List<Long> authorIds = List.of(999L);

            Category category = new Category();
            category.setId(categoryId);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(authorRepository.findAllById(any())).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> bookService.create("Title", "978-0-123", categoryId, authorIds))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("One or more authors not found");

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("Should throw DataIntegrityViolationException when ISBN already exists")
        void shouldThrowDataIntegrityViolationExceptionWhenIsbnAlreadyExists() {
            // Arrange
            String isbn = "978-0134685991";
            Long categoryId = 1L;
            List<Long> authorIds = List.of(1L);

            Category category = new Category();
            category.setId(categoryId);

            Author author = new Author();
            author.setId(1L);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(authorRepository.findAllById(any())).thenReturn(List.of(author));
            when(bookRepository.save(any(Book.class)))
                    .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

            // Act & Assert
            assertThatThrownBy(() -> bookService.create("Title", isbn, categoryId, authorIds))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Book with ISBN '" + isbn + "' already exists");
        }
    }

    @Nested
    @DisplayName("Find Book Tests")
    class FindBookTests {

        @Test
        @DisplayName("Should find book by ID with authors and category")
        void shouldFindBookByIdWithAuthorsAndCategory() {
            // Arrange
            Long bookId = 1L;
            Book book = createBookEntity(bookId, "Effective Java", "978-0134685991", new Category(), Set.of());
            when(bookRepository.findByIdWithAuthorsAndCategory(bookId)).thenReturn(Optional.of(book));

            // Act
            Book result = bookService.findById(bookId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(bookId);
            verify(bookRepository).findByIdWithAuthorsAndCategory(bookId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when book ID not found")
        void shouldThrowResourceNotFoundExceptionWhenBookIdNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            when(bookRepository.findByIdWithAuthorsAndCategory(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.findById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with id: " + nonExistentId);
        }

        @Test
        @DisplayName("Should find book by ISBN with authors and category")
        void shouldFindBookByIsbnWithAuthorsAndCategory() {
            // Arrange
            String isbn = "978-0134685991";
            Book book = createBookEntity(1L, "Effective Java", isbn, new Category(), Set.of());
            when(bookRepository.findByIsbnWithAuthorsAndCategory(isbn)).thenReturn(Optional.of(book));

            // Act
            Book result = bookService.findByIsbn(isbn);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getIsbn()).isEqualTo(isbn);
            verify(bookRepository).findByIsbnWithAuthorsAndCategory(isbn);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ISBN not found")
        void shouldThrowResourceNotFoundExceptionWhenIsbnNotFound() {
            // Arrange
            String nonExistentIsbn = "978-0-000-00000-0";
            when(bookRepository.findByIsbnWithAuthorsAndCategory(nonExistentIsbn)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.findByIsbn(nonExistentIsbn))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with ISBN: " + nonExistentIsbn);
        }
    }

    @Nested
    @DisplayName("Update Book Tests")
    class UpdateBookTests {

        @Test
        @DisplayName("Should update book and sync author relationships")
        void shouldUpdateBookAndSyncAuthorRelationships() {
            // Arrange
            Long bookId = 1L;
            String newTitle = "Updated Title";
            String newIsbn = "978-0-123456-78-9";
            Long newCategoryId = 2L;
            List<Long> newAuthorIds = List.of(2L, 3L);

            Category oldCategory = new Category();
            oldCategory.setId(1L);

            Book existingBook = createBookEntity(bookId, "Old Title", "978-0-old", oldCategory, new java.util.HashSet<>());

            Category newCategory = new Category();
            newCategory.setId(newCategoryId);
            newCategory.setName("Science");

            Author author2 = new Author();
            author2.setId(2L);
            Author author3 = new Author();
            author3.setId(3L);

            Book updatedBook = createBookEntity(bookId, newTitle, newIsbn, newCategory, Set.of(author2, author3));

            when(bookRepository.findByIdWithAuthorsAndCategory(bookId)).thenReturn(Optional.of(existingBook));
            when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
            when(authorRepository.findAllById(any())).thenReturn(List.of(author2, author3));
            when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);

            // Act
            Book result = bookService.update(bookId, newTitle, newIsbn, newCategoryId, newAuthorIds);

            // Assert
            assertThat(result.getTitle()).isEqualTo(newTitle);
            assertThat(result.getIsbn()).isEqualTo(newIsbn);
            assertThat(result.getCategory().getId()).isEqualTo(newCategoryId);
            assertThat(result.getAuthors()).hasSize(2);
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent book")
        void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentBook() {
            // Arrange
            Long nonExistentId = 999L;
            when(bookRepository.findByIdWithAuthorsAndCategory(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            List<Long> authorIds = List.of(1L);
            assertThatThrownBy(() -> bookService.update(nonExistentId, "Title", "978-0-123", 1L, authorIds))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with id: " + nonExistentId);
        }

        @Test
        @DisplayName("Should throw DataIntegrityViolationException when updating to duplicate ISBN")
        void shouldThrowDataIntegrityViolationExceptionWhenUpdatingToDuplicateIsbn() {
            // Arrange
            Long bookId = 1L;
            Book existingBook = createBookEntity(bookId, "Title", "978-0-old", new Category(), new java.util.HashSet<>());

            Category category = new Category();
            category.setId(1L);
            Author author = new Author();
            author.setId(1L);

            when(bookRepository.findByIdWithAuthorsAndCategory(bookId)).thenReturn(Optional.of(existingBook));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(authorRepository.findAllById(any())).thenReturn(List.of(author));
            when(bookRepository.save(any(Book.class)))
                    .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

            // Act & Assert
            List<Long> authorIds = List.of(1L);
            assertThatThrownBy(() -> bookService.update(bookId, "Title", "978-0-new", 1L, authorIds))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Book with ISBN '978-0-new' already exists");
        }
    }

    @Nested
    @DisplayName("Deactivate Book Tests")
    class DeactivateBookTests {

        @Test
        @DisplayName("Should deactivate book with DISPONIVEL status")
        void shouldDeactivateBookWithDisponivelStatus() {
            // Arrange
            Long bookId = 1L;
            Book book = createBookEntity(bookId, "Title", "978-0-123", new Category(), Set.of());
            book.setStatus(BookStatus.DISPONIVEL);

            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

            // Act
            bookService.deactivate(bookId);

            // Assert
            assertThat(book.getActive()).isFalse();
            verify(bookRepository).findById(bookId);
        }

        @Test
        @DisplayName("Should throw BookLoanedException when deactivating EMPRESTADO book")
        void shouldThrowBookLoanedExceptionWhenDeactivatingEmprestadoBook() {
            // Arrange
            Long bookId = 1L;
            Book book = createBookEntity(bookId, "Title", "978-0-123", new Category(), Set.of());
            book.setStatus(BookStatus.EMPRESTADO);

            when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

            // Act & Assert
            assertThatThrownBy(() -> bookService.deactivate(bookId))
                    .isInstanceOf(BookLoanedException.class)
                    .hasMessageContaining("Cannot inactivate book with id " + bookId)
                    .hasMessageContaining("it is currently loaned");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deactivating non-existent book")
        void shouldThrowResourceNotFoundExceptionWhenDeactivatingNonExistentBook() {
            // Arrange
            Long nonExistentId = 999L;
            when(bookRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.deactivate(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Book not found with id: " + nonExistentId);
        }
    }

    @Nested
    @DisplayName("List Books Tests")
    class ListBooksTests {

        @Test
        @DisplayName("Should return paginated list of books")
        void shouldReturnPaginatedListOfBooks() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            Category category = new Category();
            category.setId(1L);
            category.setName("Fiction");

            Book book1 = createBookEntity(1L, "Book One", "978-0-111", category, Set.of());
            Book book2 = createBookEntity(2L, "Book Two", "978-0-222", category, Set.of());

            Page<Book> bookPage = new PageImpl<>(List.of(book1, book2), pageable, 2);
            when(bookRepository.findAllWithCategory(pageable)).thenReturn(bookPage);

            // Act
            Page<Book> result = bookService.list(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Book One");
            verify(bookRepository).findAllWithCategory(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no books exist")
        void shouldReturnEmptyPageWhenNoBooksExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(bookRepository.findAllWithCategory(pageable)).thenReturn(emptyPage);

            // Act
            Page<Book> result = bookService.list(pageable);

            // Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    private Book createBookEntity(Long id, String title, String isbn, Category category, Set<Author> authors) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setCategory(category);
        book.setAuthors(new java.util.HashSet<>(authors));
        book.setStatus(BookStatus.DISPONIVEL);
        book.setActive(true);
        book.setCreatedAt(Instant.now());
        book.setUpdatedAt(Instant.now());
        return book;
    }
}
