package github.oliveira.gb.librarycatalogapi.domain.loan;

/**
 * Enum representing the lifecycle status of a loan transaction.
 * ATIVO: loan is currently open, books have not been fully returned.
 * FINALIZADO: all items returned, loan is closed.
 */
public enum LoanStatus {
    ATIVO,
    FINALIZADO
}
