package github.oliveira.gb.librarycatalogapi.domain.catalog;

import github.oliveira.gb.librarycatalogapi.api.catalog.CatalogSearchResponse;
import github.oliveira.gb.librarycatalogapi.api.catalog.IsbnPublicResponse;
import github.oliveira.gb.librarycatalogapi.domain.book.BookRepository;
import github.oliveira.gb.librarycatalogapi.domain.book.BookStatus;
import github.oliveira.gb.librarycatalogapi.infrastructure.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for catalog search and public ISBN lookup operations.
 * All query methods are read-only and enforce the 100-record hard limit.
 */
@Service
public class CatalogService {

    private static final int HARD_LIMIT = 100;

    private final BookRepository bookRepository;

    public CatalogService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Searches the catalog with optional dynamic filters.
     * Returns a flat projection with concatenated authors.
     * Enforces a maximum of 100 records at the database level.
     *
     * @param title partial title filter
     * @param author partial author filter
     * @param genre exact category name filter
     * @param available filter by DISPONIVEL status when true
     * @return list of catalog search responses (max 100)
     */
    @Transactional(readOnly = true)
    public List<CatalogSearchResponse> search(String title, String author, String genre, Boolean available) {
        String statusFilter = Boolean.TRUE.equals(available) ? BookStatus.DISPONIVEL.name() : null;
        List<Object[]> rows = bookRepository.searchCatalogNative(title, author, genre, statusFilter, HARD_LIMIT);
        return rows.stream().map(this::mapToCatalogSearchResponse).toList();
    }

    /**
     * Looks up a book by ISBN for the public endpoint.
     * Returns a security-isolated projection without primary key or stock data.
     *
     * @param isbn the book ISBN
     * @return the public ISBN response
     * @throws ResourceNotFoundException if the ISBN is not found
     */
    @Transactional(readOnly = true)
    public IsbnPublicResponse findByIsbn(String isbn) {
        List<Object[]> rows = bookRepository.findByIsbnNative(isbn);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Book not found for ISBN: " + isbn);
        }
        return mapToIsbnPublicResponse(rows.getFirst());
    }

    private CatalogSearchResponse mapToCatalogSearchResponse(Object[] row) {
        return new CatalogSearchResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (Boolean) row[5]
        );
    }

    private IsbnPublicResponse mapToIsbnPublicResponse(Object[] row) {
        return new IsbnPublicResponse(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3]
        );
    }
}
