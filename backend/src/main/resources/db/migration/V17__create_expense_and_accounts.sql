-- Step 15: Expense Tracking & Accounts Summary (দৈনিক খরচ ভাউচার ও হিসাব বিবরণী)

CREATE TABLE expense_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(30) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expense_categories_name UNIQUE (name),
    CONSTRAINT uk_expense_categories_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    voucher_number VARCHAR(50) NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    expense_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payee_name VARCHAR(100) NOT NULL,
    reference_no VARCHAR(100),
    recorded_by VARCHAR(100) NOT NULL,
    notes VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_expenses_voucher UNIQUE (voucher_number),
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_expenses_date ON expenses (expense_date);
CREATE INDEX idx_expenses_category ON expenses (category_id);

-- Initial Seed Data: বাংলাদেশের শিক্ষা প্রতিষ্ঠানের বহুল ব্যবহৃত ব্যয়ের খাত
INSERT INTO expense_categories (name, code, description, is_active, created_at, updated_at) VALUES
('শিক্ষক ও কর্মকর্তা বেতন / Salary', 'SALARY', 'শিক্ষক, কর্মকর্তা ও কর্মচারীদের মাসিক বেতন ও সম্মানী', TRUE, NOW(6), NOW(6)),
('বিদ্যুৎ, গ্যাস ও ইউটিলিটি / Utility', 'UTILITY', 'বিদ্যুৎ বিল, গ্যাস, পানি ও ওয়াসা সেবা খরচ', TRUE, NOW(6), NOW(6)),
('অফিস স্টেশনারি ও মুদ্রণ / Stationery', 'STATIONERY', 'কাগজ, কলম, চক, ডাস্টার ও দাপ্তরিক ফাইলপত্র', TRUE, NOW(6), NOW(6)),
('মেরামত ও অবকাঠামো / Maintenance', 'MAINTENANCE', 'ভবন সংস্কার, বৈদ্যুতিক সরঞ্জাম ও ফার্নিচার মেরামত', TRUE, NOW(6), NOW(6)),
('আইটি ও ইন্টারনেট / IT & Internet', 'IT_INTERNET', 'ব্রডব্যান্ড সংযোগ, এসএমএস গেটওয়ে ও সফটওয়্যার লাইসেন্স', TRUE, NOW(6), NOW(6)),
('পরীক্ষা সংক্রান্ত ব্যয় / Exam Printing', 'EXAM_COST', 'প্রশ্নপত্র ও উত্তরপত্র মুদ্রণ এবং মূল্যায়ন খরচ', TRUE, NOW(6), NOW(6)),
('খেলাধুলা ও সাংস্কৃতিক / Events', 'EVENTS', 'বার্ষিক ক্রীড়া প্রতিযোগিতা, জাতীয় দিবস ও পুরস্কার', TRUE, NOW(6), NOW(6)),
('বিবিধ পরিচালন ব্যয় / Miscellaneous', 'MISC', 'চা-নাস্তা, আপ্যায়ন ও তাৎক্ষণিক খুচরা ব্যয়', TRUE, NOW(6), NOW(6));
