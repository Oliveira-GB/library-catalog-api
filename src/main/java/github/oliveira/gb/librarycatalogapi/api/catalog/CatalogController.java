package github.oliveira.gb.librarycatalogapi.api.catalog;

import github.oliveira.gb.librarycatalogapi.domain.catalog.CatalogService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for public catalog discovery and ISBN lookup endpoints.
 * Follows REST conventions and returns appropriate HTTP status codes.
 * The ISBN endpoint is publicly accessible (permitAll).
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Searches the catalog with optional dynamic filters.
     * Returns a lightweight flat projection (max 100 records).
     *
     * @param title partial title filter
     * @param author partial author filter
     * @param genre exact category name filter
     * @param available filter by available status when true
     * @return ResponseEntity with list of catalog search results (HTTP 200)
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CatalogSearchResponse>> search(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Boolean disponivel) {
        List<CatalogSearchResponse> results = catalogService.search(titulo, autor, genero, disponivel);
        return ResponseEntity.ok(results);
    }

    /**
     * Public endpoint for looking up a book by its ISBN.
     * Does not require authentication.
     * Returns a security-isolated projection.
     *
     * @param isbn the book ISBN
     * @return ResponseEntity with book metadata (HTTP 200)
     */
    @GetMapping(value = "/livros/{isbn}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IsbnPublicResponse> findByIsbn(@PathVariable String isbn) {
        IsbnPublicResponse response = catalogService.findByIsbn(isbn);
        return ResponseEntity.ok(response);
    }
}
