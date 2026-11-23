-- Step 16: SMS & Email Notification Engine (নোটিফিকেশন টেমপ্লেট, বাল্ক এসএমএস ও ব্রডকাস্ট)

-- ১. নোটিফিকেশন লগ টেবিলের ফরেন কি সুরক্ষার জন্য প্রথমে ইনডেক্স তৈরি, তারপর ইউনিক ইনডেক্স ও চেক কনস্ট্রেইন্ট প্রত্যাহার
CREATE INDEX idx_notif_student_id ON notification_logs (student_id);
ALTER TABLE notification_logs DROP INDEX uk_notification_once;
CREATE INDEX idx_notif_student_type_date ON notification_logs (student_id, notification_type, event_date);
ALTER TABLE notification_logs DROP CHECK ck_notification_type;

-- ২. নোটিফিকেশন টেমপ্লেট টেবিল
CREATE TABLE notification_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'SMS',
    category VARCHAR(30) NOT NULL,
    subject VARCHAR(150),
    template_body TEXT NOT NULL,
    variables VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_templates_name UNIQUE (name),
    CONSTRAINT uk_notification_templates_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ৩. নোটিফিকেশন ক্যাম্পেইন ও ব্রডকাস্ট হিস্ট্রি টেবিল
CREATE TABLE notification_campaigns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'SMS',
    target_audience VARCHAR(30) NOT NULL,
    target_class_id BIGINT,
    target_section_id BIGINT,
    message TEXT NOT NULL,
    total_recipients INT NOT NULL DEFAULT 0,
    sent_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    created_by VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_campaign_class FOREIGN KEY (target_class_id) REFERENCES classes (id),
    CONSTRAINT fk_campaign_section FOREIGN KEY (target_section_id) REFERENCES sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_campaign_created ON notification_campaigns (created_at);

-- ৪. Initial Seed Data: বাংলাদেশের শিক্ষা প্রতিষ্ঠানের বহুল ব্যবহৃত নোটিফিকেশন টেমপ্লেট
INSERT INTO notification_templates (name, code, channel, category, subject, template_body, variables, is_active, created_at, updated_at) VALUES
('বকেয়া ফি তাগাদা বার্তা', 'FEE_REMINDER_BN', 'SMS', 'FEE_DUE', 'বকেয়া ফি পরিশোধের অনুরোধ', 'প্রিয় অভিভাবক, {student_name} ({class_name}, রোল: {roll_no})-এর ৳{due_amount} ফি বকেয়া রয়েছে। অনুগ্রহ করে দ্রুত পরিশোধ করুন। - {school_name}', 'student_name,class_name,roll_no,due_amount,school_name', TRUE, NOW(6), NOW(6)),
('অনুপস্থিতি অ্যালার্ট', 'ABSENCE_ALERT_BN', 'SMS', 'ABSENCE', 'অনুপস্থিতি নোটিশ', 'প্রিয় অভিভাবক, আপনার সন্তান {student_name} ({class_name}) আজ {date} বিদ্যালয়ে অনুপস্থিত। - {school_name}', 'student_name,class_name,date,school_name', TRUE, NOW(6), NOW(6)),
('পরীক্ষার নোটিশ ও রুটিন', 'EXAM_NOTICE_BN', 'SMS', 'EXAM_NOTICE', 'পরীক্ষার সময়সূচি সংক্রান্ত', 'প্রিয় অভিভাবক, আগামী {date} তারিখ হতে {exam_name} শুরু হতে যাচ্ছে। বিস্তারিত সময়সূচি বিদ্যালয় নোটিশে দেখুন। - {school_name}', 'student_name,exam_name,date,school_name', TRUE, NOW(6), NOW(6)),
('পরীক্ষার ফলাফল প্রকাশ', 'RESULT_PUBLISH_BN', 'SMS', 'RESULT_PUBLISH', 'পরীক্ষার ফলাফল প্রকাশ সংক্রান্ত', 'প্রিয় অভিভাবক, {exam_name}-এর ফলাফল প্রকাশিত হয়েছে। ফলাফল মার্কশিট অনলাইন বা বিদ্যালয় হতে সংগ্রহ করুন। - {school_name}', 'student_name,exam_name,school_name', TRUE, NOW(6), NOW(6)),
('সাধারণ নোটিশ ও জরুরি ঘোষণা', 'ANNOUNCEMENT_BN', 'SMS', 'ANNOUNCEMENT', 'বিদ্যালয়ের জরুরি নোটিশ', 'সম্মানিত অভিভাবক, {message} - {school_name}', 'message,school_name', TRUE, NOW(6), NOW(6));
