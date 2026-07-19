ALTER TABLE transfers ADD COLUMN idempotency_key VARCHAR(36);
CREATE UNIQUE INDEX uk_transfers_idempotency_key ON transfers (idempotency_key);

CREATE TABLE account_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    transfer_id BIGINT,
    entry_type VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(50),
    idempotency_key VARCHAR(36),
    CONSTRAINT fk_account_entries_account
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_account_entries_transfer
        FOREIGN KEY (transfer_id) REFERENCES transfers (id),
    CONSTRAINT uk_account_entries_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_account_entries_account_created_at
    ON account_entries (account_id, created_at);

INSERT INTO account_entries (
    account_id, transfer_id, entry_type, amount, currency, description, created_at
)
SELECT source_account_id, id, 'TRANSFER_OUT', amount, currency, description, created_at
FROM transfers
WHERE status = 'COMPLETED';

INSERT INTO account_entries (
    account_id, transfer_id, entry_type, amount, currency, description, created_at
)
SELECT target_account_id, id, 'TRANSFER_IN', amount, currency, description, created_at
FROM transfers
WHERE status = 'COMPLETED';
