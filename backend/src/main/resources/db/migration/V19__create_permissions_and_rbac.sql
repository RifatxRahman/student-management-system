-- Step 17: User Roles, RBAC & Fine-Grained Permissions (পারমিশন ম্যাট্রিক্স ও ডাইনামিক রোল)

-- ১. ইউজার রোল সীমাবদ্ধতা শিথিল ও নতুন রোল সংযোজন
ALTER TABLE users DROP CHECK ck_users_role;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'TEACHER', 'ACCOUNTANT', 'STAFF'));

-- ২. পারমিশন মাস্টার টেবিল
CREATE TABLE permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ৩. রোল ভিত্তিক ডিফল্ট পারমিশন ম্যাপিং টেবিল
CREATE TABLE role_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role VARCHAR(20) NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role, permission_id),
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ৪. ইউজার ভিত্তিক কাস্টম পারমিশন ওভাররাইড টেবিল (গ্রান্ট বা রিভোক)
CREATE TABLE user_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_permission UNIQUE (user_id, permission_id),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_role_permissions_role ON role_permissions (role);
CREATE INDEX idx_user_permissions_user ON user_permissions (user_id);

-- ৫. Initial Seed Data: স্ট্যান্ডার্ড পারমিশন ও মডিউল
INSERT INTO permissions (name, code, module, description, created_at, updated_at) VALUES
('শিক্ষার্থী তথ্য পরিচালনা', 'STUDENT_MANAGE', 'STUDENTS', 'শিক্ষার্থী ভর্তি, প্রোফাইল এডিট ও অভিভাবক ব্যবস্থাপনা', NOW(6), NOW(6)),
('শিক্ষার্থী হাজিরা গ্রহণ', 'ATTENDANCE_MANAGE', 'ATTENDANCE', 'দৈনিক হাজিরা শিট এন্ট্রি, ফাইনাল ও আপডেট', NOW(6), NOW(6)),
('রুটিন ও ক্যালেন্ডার', 'ROUTINE_CALENDAR_MANAGE', 'ACADEMIC', 'ক্লাস রুটিন, টাইম-স্লট ও একাডেমিক ইভেন্ট ব্যবস্থাপনা', NOW(6), NOW(6)),
('পরীক্ষা ও মার্কস এন্ট্রি', 'EXAM_MANAGE', 'EXAMS', 'পরীক্ষা আয়োজন, বিষয় বরাদ্দ ও মার্কস এন্ট্রি', NOW(6), NOW(6)),
('ফলাফল প্রকাশ ও ট্যাবুলেশন', 'RESULT_PUBLISH', 'EXAMS', 'ট্যাবুলেশন শিট প্রস্তুত, জিপিএ মূল্যায়ন ও ফল প্রকাশ', NOW(6), NOW(6)),
('ফি কাঠামো ও ছাড় নির্ধারণ', 'FEES_MANAGE', 'FINANCE', 'শ্রেণিভিত্তিক ফি কাঠামো সেটআপ ও শিক্ষার্থী ওয়েভার', NOW(6), NOW(6)),
('ফি আদায় ও ইনভয়েস', 'FEES_COLLECT', 'FINANCE', 'ইনভয়েস তৈরি, ফি আদায় ও মানি রসিদ প্রদান', NOW(6), NOW(6)),
('দৈনিক পরিচালন ব্যয়', 'EXPENSE_MANAGE', 'FINANCE', 'দৈনিক খরচ ভাউচার তৈরি ও ব্যয়ের খাত ব্যবস্থাপনা', NOW(6), NOW(6)),
('আর্থিক স্থিতি ও লাভ-ক্ষতি', 'ACCOUNTS_REPORT', 'FINANCE', 'আয়-ব্যয় সারাংশ, অনাদায়ী বকেয়া ও ক্যাশ/ব্যাংক রিপোর্ট', NOW(6), NOW(6)),
('বাল্ক এসএমএস ও নোটিফিকেশন', 'SMS_BROADCAST', 'COMMUNICATION', 'নোটিফিকেশন টেমপ্লেট ও বাল্ক বার্তা সম্প্রচার', NOW(6), NOW(6)),
('ইউজার ও পারমিশন প্রশাসন', 'USER_MANAGE', 'SYSTEM', 'ব্যবহারকারী তৈরি, পাসওয়ার্ড রিসেট ও রোল পারমিশন নির্ধারণ', NOW(6), NOW(6));

-- ৬. রোল-ভিত্তিক ডিফল্ট পারমিশন ম্যাপিং
-- ADMIN: সমস্ত পারমিশন
INSERT INTO role_permissions (role, permission_id, created_at, updated_at)
SELECT 'ADMIN', id, NOW(6), NOW(6) FROM permissions;

-- TEACHER: হাজিরা, রুটিন, পরীক্ষা ও মার্কস এন্ট্রি
INSERT INTO role_permissions (role, permission_id, created_at, updated_at)
SELECT 'TEACHER', id, NOW(6), NOW(6) FROM permissions
WHERE code IN ('ATTENDANCE_MANAGE', 'ROUTINE_CALENDAR_MANAGE', 'EXAM_MANAGE');

-- ACCOUNTANT: ফি কাঠামো, ফি আদায়, ব্যয় ভাউচার ও আর্থিক বিবরণী
INSERT INTO role_permissions (role, permission_id, created_at, updated_at)
SELECT 'ACCOUNTANT', id, NOW(6), NOW(6) FROM permissions
WHERE code IN ('FEES_MANAGE', 'FEES_COLLECT', 'EXPENSE_MANAGE', 'ACCOUNTS_REPORT');

-- STAFF: শিক্ষার্থী ভর্তি, হাজিরা, ফি আদায় ও এসএমএস নোটিফিকেশন
INSERT INTO role_permissions (role, permission_id, created_at, updated_at)
SELECT 'STAFF', id, NOW(6), NOW(6) FROM permissions
WHERE code IN ('STUDENT_MANAGE', 'ATTENDANCE_MANAGE', 'FEES_COLLECT', 'SMS_BROADCAST');
