package github.oliveira.gb.librarycatalogapi.domain.category;

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
 * Unit tests for CategoryService.
 * Tests business logic including uniqueness validation and state initialization.
 * Following TDD approach as required by project standards.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Category Service Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category with active status set to true")
        void shouldCreateCategoryWithActiveStatusSetToTrue() {
            // Arrange
            String categoryName = "Fiction";

            Category savedCategory = new Category();
            savedCategory.setId(1L);
            savedCategory.setName(categoryName);
            savedCategory.setActive(true);
            savedCategory.setCreatedAt(Instant.now());
            savedCategory.setUpdatedAt(Instant.now());

            when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

            // Act
            Category result = categoryService.create(categoryName);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo(categoryName);
            assertThat(result.getActive()).isTrue();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw exception when category name already exists")
        void shouldThrowExceptionWhenCategoryNameAlreadyExists() {
            // Arrange
            String existingName = "Fiction";
            when(categoryRepository.save(any(Category.class)))
                    .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.create(existingName))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("Category with name '" + existingName + "' already exists");

            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should trim category name before saving")
        void shouldTrimCategoryNameBeforeSaving() {
            // Arrange
            String nameWithWhitespace = "  Fiction  ";
            String trimmedName = "Fiction";

            Category savedCategory = new Category();
            savedCategory.setId(1L);
            savedCategory.setName(trimmedName);
            savedCategory.setActive(true);

            when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

            // Act
            Category result = categoryService.create(nameWithWhitespace);

            // Assert
            assertThat(result.getName()).isEqualTo(trimmedName);
            verify(categoryRepository).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("List Categories Tests")
    class ListCategoriesTests {

        @Test
        @DisplayName("Should return paginated list of categories")
        void shouldReturnPaginatedListOfCategories() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            
            Category category1 = new Category();
            category1.setId(1L);
            category1.setName("Fiction");
            category1.setActive(true);
            
            Category category2 = new Category();
            category2.setId(2L);
            category2.setName("Science");
            category2.setActive(true);
            
            Page<Category> categoryPage = new PageImpl<>(List.of(category1, category2), pageable, 2);
            when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);

            // Act
            Page<Category> result = categoryService.list(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Fiction");
            assertThat(result.getContent().get(1).getName()).isEqualTo("Science");
            verify(categoryRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no categories exist")
        void shouldReturnEmptyPageWhenNoCategoriesExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(categoryRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            Page<Category> result = categoryService.list(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void shouldRespectPaginationParameters() {
            // Arrange
            Pageable pageable = PageRequest.of(2, 5);
            Page<Category> categoryPage = new PageImpl<>(List.of(), pageable, 0);
            when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);

            // Act
            Page<Category> result = categoryService.list(pageable);

            // Assert
            assertThat(result.getNumber()).isEqualTo(2);
            assertThat(result.getSize()).isEqualTo(5);
        }
    }
}
