-- Step 13: Fee Structure Setup (ফি খাত ও শ্রেণিভিত্তিক ফি নির্ধারণ)

CREATE TABLE fee_heads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_fee_heads_name UNIQUE (name),
    CONSTRAINT uk_fee_heads_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_structures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    fee_head_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    due_day INT NOT NULL DEFAULT 10,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_fee_structures_session_class_head UNIQUE (session_id, class_id, fee_head_id),
    CONSTRAINT fk_fee_structures_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
    CONSTRAINT fk_fee_structures_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_fee_structures_head FOREIGN KEY (fee_head_id) REFERENCES fee_heads (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial Seed Data: বাংলাদেশের স্কুল ও কলেজের বহুল ব্যবহৃত ফি খাত
INSERT INTO fee_heads (name, code, description, is_active, created_at, updated_at) VALUES
('মাসিক বেতন / Tuition Fee', 'TUITION', 'সাধারণ মাসিক টিউশন ফি', TRUE, NOW(6), NOW(6)),
('ভর্তি ফি / Admission Fee', 'ADMISSION', 'নতুন শিক্ষার্থী ভর্তি বা বাৎসরিক রি-এডমিশন ফি', TRUE, NOW(6), NOW(6)),
('পরীক্ষা ফি / Exam Fee', 'EXAM', 'টার্ম ও সেমিস্টার মূল্যায়ন পরীক্ষা ফি', TRUE, NOW(6), NOW(6)),
('সেশন চার্জ / Session Fee', 'SESSION', 'বার্ষিক উন্নয়ন, ক্রীড়া ও সেশন চার্জ', TRUE, NOW(6), NOW(6)),
('লাইব্রেরি ও ল্যাব ফি / Library & Lab', 'LIBRARY_LAB', 'ব্যবহারিক বিজ্ঞানাগার ও পাঠাগার উন্নয়ন ফি', TRUE, NOW(6), NOW(6)),
('কম্পিউটার ও আইসিটি ফি / ICT Fee', 'ICT', 'ডিজিটাল ল্যাব ও কম্পিউটার ল্যাব ব্যবহার ফি', TRUE, NOW(6), NOW(6));
