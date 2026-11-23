-- Step 14: Fee Collection & Student Invoice Generation (ইনভয়েস তৈরি, পেমেন্ট ও ওয়েভার)

-- ১. শিক্ষার্থীর বিশেষ ওয়েভার/বৃত্তি
CREATE TABLE student_waivers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    fee_head_id BIGINT NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    reason VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_student_waiver UNIQUE (student_id, session_id, fee_head_id),
    CONSTRAINT fk_waivers_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_waivers_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_waivers_head FOREIGN KEY (fee_head_id) REFERENCES fee_heads (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ২. শিক্ষার্থীর ফি ইনভয়েস
CREATE TABLE invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(50) NOT NULL,
    student_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    billing_month VARCHAR(10) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal_amount DECIMAL(10,2) NOT NULL,
    waiver_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    due_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    notes VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_number UNIQUE (invoice_number),
    CONSTRAINT uk_student_invoice_month_title UNIQUE (student_id, session_id, billing_month, title),
    CONSTRAINT fk_invoices_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_invoices_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_invoices_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_invoices_section FOREIGN KEY (section_id) REFERENCES sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ৩. ইনভয়েসের খাতভিত্তিক আইটেম ব্রেকডাউন
CREATE TABLE invoice_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    fee_head_id BIGINT NOT NULL,
    fee_head_name VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    waiver_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(10,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_items_head FOREIGN KEY (fee_head_id) REFERENCES fee_heads (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ৪. ফি পরিশোধ ও পেমেন্ট রসিদ
CREATE TABLE fee_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_number VARCHAR(50) NOT NULL,
    invoice_id BIGINT NOT NULL,
    payment_date DATETIME(6) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(100),
    collected_by VARCHAR(100) NOT NULL,
    notes VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_fee_payments_receipt UNIQUE (receipt_number),
    CONSTRAINT fk_fee_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_invoices_student ON invoices (student_id);
CREATE INDEX idx_invoices_session_class ON invoices (session_id, class_id);
CREATE INDEX idx_invoices_status ON invoices (status);
CREATE INDEX idx_fee_payments_date ON fee_payments (payment_date);
