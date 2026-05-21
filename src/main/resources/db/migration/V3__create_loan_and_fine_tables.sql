-- V3 Migration: Create Loan, LoanItem, and Fine tables
-- Structural prerequisite for Epic 3 (Transactional Loan and Return Engine)

-- Table: loans
-- Represents a batch loan transaction. Uses lifecycle status (ATIVO, FINALIZADO)
-- instead of soft delete to preserve full historical queryability.
CREATE TABLE loans (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ATIVO',
    due_date TIMESTAMP NOT NULL,
    returned_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_reader FOREIGN KEY (reader_id) REFERENCES readers(id)
);

-- Table: loan_items
-- Represents an individual book within a loan batch.
-- Separate entity (not a join table) to support per-item tracking
-- (return date, fine amount) required for US 3.3 (Return and Fine Settlement).
CREATE TABLE loan_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    returned_at TIMESTAMP,
    fine_amount NUMERIC(10, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_item_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_item_book FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Table: fines
-- Tracks financial penalties associated with a reader.
-- No soft delete applied to preserve full debt history visibility.
CREATE TABLE fines (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reader_id BIGINT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    paid BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fine_reader FOREIGN KEY (reader_id) REFERENCES readers(id)
);

-- Indexes for performance
CREATE INDEX idx_loan_reader ON loans(reader_id);
CREATE INDEX idx_loan_status ON loans(status);
CREATE INDEX idx_loan_due_date ON loans(due_date);
CREATE INDEX idx_loan_item_loan ON loan_items(loan_id);
CREATE INDEX idx_loan_item_book ON loan_items(book_id);
CREATE INDEX idx_fine_reader ON fines(reader_id);
CREATE INDEX idx_fine_paid ON fines(paid);
