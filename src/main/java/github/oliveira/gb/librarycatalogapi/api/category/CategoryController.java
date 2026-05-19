package github.oliveira.gb.librarycatalogapi.api.category;

import github.oliveira.gb.librarycatalogapi.domain.category.Category;
import github.oliveira.gb.librarycatalogapi.domain.category.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for Category API endpoints.
 * Handles HTTP requests for category management operations.
 * All endpoints follow REST conventions and return appropriate HTTP status codes.
 */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Constructs a new CategoryController with required dependencies.
     *
     * @param categoryService the service for category operations
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Creates a new category.
     *
     * @param request the category creation request
     * @return ResponseEntity with created category and Location header (HTTP 201)
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.create(request.name());
        CategoryResponse response = toResponse(category);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(category.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Returns a paginated list of all categories.
     *
     * @param pageable the pagination parameters (page, size, sort)
     * @return ResponseEntity with paginated category list (HTTP 200)
     */
    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> list(Pageable pageable) {
        Page<Category> categories = categoryService.list(pageable);
        Page<CategoryResponse> response = categories.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Converts a Category entity to CategoryResponse DTO.
     *
     * @param category the category entity
     * @return the category response DTO
     */
    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
