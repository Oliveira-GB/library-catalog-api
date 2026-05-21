package github.oliveira.gb.librarycatalogapi.api.loan;

import github.oliveira.gb.librarycatalogapi.domain.loan.Loan;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting between Loan entities and LoanResponse DTOs.
 */
@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan loan) {
        List<Long> bookIds = loan.getItems().stream()
                .map(item -> item.getBook().getId())
                .toList();

        return new LoanResponse(
                loan.getId(),
                loan.getReader().getId(),
                loan.getStatus().name(),
                loan.getDueDate(),
                bookIds,
                loan.getCreatedAt()
        );
    }
}
