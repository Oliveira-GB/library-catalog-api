package github.oliveira.gb.librarycatalogapi.domain.author;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthorService.
 * Tests business logic including email uniqueness validation and state initialization.
 * Following TDD approach as required by project standards.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Author Service Unit Tests")
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    @Nested
    @DisplayName("Create Author Tests")
    class CreateAuthorTests {

        @Test
        @DisplayName("Should create author with active status set to true")
        void shouldCreateAuthorWithActiveStatusSetToTrue() {
            // Arrange
            String name = "John Doe";
            String email = "john.doe@example.com";
            String biography = "A famous author";

            Author savedAuthor = new Author();
            savedAuthor.setId(1L);
            savedAuthor.setName(name);
            savedAuthor.setEmail(email);
            savedAuthor.setBiography(biography);
            savedAuthor.setActive(true);
            savedAuthor.setCreatedAt(Instant.now());
            savedAuthor.setUpdatedAt(Instant.now());

            when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

            // Act
            Author result = authorService.create(name, email, biography);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo(name);
            assertThat(result.getEmail()).isEqualTo(email);
            assertThat(result.getBiography()).isEqualTo(biography);
            assertThat(result.getActive()).isTrue();
            verify(authorRepository).save(any(Author.class));
        }

        @Test
        @DisplayName("Should create author without biography")
        void shouldCreateAuthorWithoutBiography() {
            // Arrange
            String name = "Jane Smith";
            String email = "jane.smith@example.com";

            Author savedAuthor = new Author();
            savedAuthor.setId(1L);
            savedAuthor.setName(name);
            savedAuthor.setEmail(email);
            savedAuthor.setBiography(null);
            savedAuthor.setActive(true);

            when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

            // Act
            Author result = authorService.create(name, email, null);

            // Assert
            assertThat(result.getBiography()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when author email already exists")
        void shouldThrowExceptionWhenAuthorEmailAlreadyExists() {
            // Arrange
            String name = "John Doe";
            String existingEmail = "john.doe@example.com";
            when(authorRepository.save(any(Author.class)))
                    .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

            // Act & Assert
            assertThatThrownBy(() -> authorService.create(name, existingEmail, null))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Author with email '" + existingEmail + "' already exists");

            verify(authorRepository).save(any(Author.class));
        }

        @Test
        @DisplayName("Should trim author name and email before saving")
        void shouldTrimAuthorNameAndEmailBeforeSaving() {
            // Arrange
            String nameWithWhitespace = "  John Doe  ";
            String emailWithWhitespace = "  john.doe@example.com  ";
            String trimmedName = "John Doe";
            String trimmedEmail = "john.doe@example.com";

            Author savedAuthor = new Author();
            savedAuthor.setId(1L);
            savedAuthor.setName(trimmedName);
            savedAuthor.setEmail(trimmedEmail);
            savedAuthor.setActive(true);

            when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

            // Act
            Author result = authorService.create(nameWithWhitespace, emailWithWhitespace, null);

            // Assert
            assertThat(result.getName()).isEqualTo(trimmedName);
            assertThat(result.getEmail()).isEqualTo(trimmedEmail);
            verify(authorRepository).save(any(Author.class));
        }
    }

    @Nested
    @DisplayName("List Authors Tests")
    class ListAuthorsTests {

        @Test
        @DisplayName("Should return paginated list of authors")
        void shouldReturnPaginatedListOfAuthors() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            Author author1 = new Author();
            author1.setId(1L);
            author1.setName("John Doe");
            author1.setEmail("john@example.com");
            author1.setActive(true);

            Author author2 = new Author();
            author2.setId(2L);
            author2.setName("Jane Smith");
            author2.setEmail("jane@example.com");
            author2.setActive(true);

            Page<Author> authorPage = new PageImpl<>(List.of(author1, author2), pageable, 2);
            when(authorRepository.findAll(pageable)).thenReturn(authorPage);

            // Act
            Page<Author> result = authorService.list(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
            assertThat(result.getContent().get(1).getName()).isEqualTo("Jane Smith");
            verify(authorRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no authors exist")
        void shouldReturnEmptyPageWhenNoAuthorsExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(authorRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            Page<Author> result = authorService.list(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void shouldRespectPaginationParameters() {
            // Arrange
            Pageable pageable = PageRequest.of(3, 15);
            Page<Author> authorPage = new PageImpl<>(List.of(), pageable, 0);
            when(authorRepository.findAll(pageable)).thenReturn(authorPage);

            // Act
            Page<Author> result = authorService.list(pageable);

            // Assert
            assertThat(result.getNumber()).isEqualTo(3);
            assertThat(result.getSize()).isEqualTo(15);
        }
    }
}
