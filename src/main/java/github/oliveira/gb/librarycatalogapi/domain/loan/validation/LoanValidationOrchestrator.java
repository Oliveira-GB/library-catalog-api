package github.oliveira.gb.librarycatalogapi.domain.loan.validation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orchestrator that executes all loan validators sequentially in a deterministic order.
 * Operates in strict fail-fast mode: the first validator to fail throws an exception,
 * aborting the sequence before any entity mutation occurs.
 *
 * <p>Validators are injected as a list and ordered via Spring's {@link org.springframework.core.annotation.Order}
 * annotation. The deterministic order is:</p>
 * <ol>
 *   <li>BatchSizeValidator</li>
 *   <li>DuplicateTitleValidator</li>
 *   <li>BookAvailabilityValidator</li>
 *   <li>PhysicalDefaultValidator</li>
 *   <li>FinancialDefaultValidator</li>
 *   <li>AntiHoardingValidator</li>
 * </ol>
 */
@Component
public class LoanValidationOrchestrator {

    private final List<LoanValidator> validators;

    public LoanValidationOrchestrator(List<LoanValidator> validators) {
        this.validators = validators;
    }

    /**
     * Executes all registered validators against the provided context.
     * Throws immediately on the first validation failure.
     *
     * @param context the validation context
     */
    public void validate(LoanValidationContext context) {
        for (LoanValidator validator : validators) {
            validator.validate(context);
        }
    }
}
