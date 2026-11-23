-- Step 21: Online Payment Gateway Integration (বিকাশ ও ডিজিটাল পেমেন্ট ট্রানজ্যাকশন)

CREATE TABLE payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trx_id VARCHAR(64) NOT NULL,
    invoice_id BIGINT NOT NULL,
    gateway_provider VARCHAR(30) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    gateway_payment_id VARCHAR(100),
    gateway_trx_id VARCHAR(100),
    customer_phone VARCHAR(20),
    idempotency_key VARCHAR(100),
    payment_url VARCHAR(500),
    error_message VARCHAR(255),
    payer_reference VARCHAR(100),
    verified_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_trx_id UNIQUE (trx_id),
    CONSTRAINT uk_payment_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_pay_txn_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pay_txn_invoice ON payment_transactions (invoice_id);
CREATE INDEX idx_pay_txn_status ON payment_transactions (status);
CREATE INDEX idx_pay_txn_gateway ON payment_transactions (gateway_provider, gateway_trx_id);
CREATE INDEX idx_pay_txn_created ON payment_transactions (created_at DESC);
