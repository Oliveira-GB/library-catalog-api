package github.oliveira.gb.librarycatalogapi.api.loan;

import github.oliveira.gb.librarycatalogapi.domain.loan.Loan;
import github.oliveira.gb.librarycatalogapi.domain.loan.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST controller for loan operations.
 */
@RestController
@RequestMapping("/api/v1/emprestimos")
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper loanMapper;

    public LoanController(LoanService loanService, LoanMapper loanMapper) {
        this.loanService = loanService;
        this.loanMapper = loanMapper;
    }

    /**
     * Creates a new batch loan.
     *
     * @param request the loan request containing readerId and bookIds
     * @return the created loan with HTTP 201
     */
    @PostMapping
    public ResponseEntity<LoanResponse> createLoan(@RequestBody @Valid LoanRequest request) {
        Loan loan = loanService.createLoan(request.readerId(), request.bookIds());
        LoanResponse response = loanMapper.toResponse(loan);

        URI location = URI.create("/api/v1/emprestimos/" + loan.getId());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Renews an existing active loan by extending its due date.
     *
     * @param id the loan identifier
     * @return the updated loan with HTTP 200
     */
    @PatchMapping("/{id}/renovacao")
    public ResponseEntity<LoanRenewalResponse> renewLoan(@PathVariable Long id) {
        Loan loan = loanService.renewLoan(id);
        LoanRenewalResponse response = loanMapper.toRenewalResponse(loan);
        return ResponseEntity.ok(response);
    }
}
