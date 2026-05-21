package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

/**
 * Strategy interface for loan validation rules.
 * Each concrete validator implements a single business rule and is executed
 * sequentially by the LoanValidationOrchestrator in fail-fast mode.
 */
public interface LoanValidator {

    /**
     * Validates the loan context against a specific business rule.
     *
     * @param context the validation context containing reader and books
     * @throws RuntimeException subclass if the rule is violated
     */
    void validate(LoanValidationContext context);
}
