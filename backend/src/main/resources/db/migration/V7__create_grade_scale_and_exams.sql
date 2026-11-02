CREATE TABLE grade_scales (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              min_percent DECIMAL(5,2) NOT NULL,
                              grade VARCHAR(10) NOT NULL,
                              grade_point DECIMAL(3,2) NOT NULL,
                              created_at DATETIME(6) NOT NULL,
                              updated_at DATETIME(6) NOT NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT uk_grade_scales_min UNIQUE (min_percent),
                              CONSTRAINT uk_grade_scales_grade UNIQUE (grade),
                              CONSTRAINT ck_grade_scales_percent CHECK (min_percent >= 0 AND min_percent <= 100),
                              CONSTRAINT ck_grade_scales_point CHECK (grade_point >= 0 AND grade_point <= 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO grade_scales (min_percent, grade, grade_point, created_at, updated_at) VALUES
                                                                                       (80.00, 'A+', 5.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                                                                                       (70.00, 'A',  4.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                                                                                       (60.00, 'A-', 3.50, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                                                                                       (50.00, 'B',  3.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                                                                                       (40.00, 'C',  2.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                                                                                       (33.00, 'D',  1.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                                                                                       (0.00,  'F',  0.00, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

CREATE TABLE exams (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       session_id BIGINT NOT NULL,
                       class_id BIGINT NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       exam_type VARCHAR(10) NOT NULL,
                       status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',
                       published_at DATETIME(6) NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_exams_session_class_name UNIQUE (session_id, class_id, name),
                       CONSTRAINT ck_exams_type CHECK (exam_type IN ('MIDTERM', 'FINAL', 'TEST')),
                       CONSTRAINT ck_exams_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
                       CONSTRAINT fk_exams_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
                       CONSTRAINT fk_exams_class FOREIGN KEY (class_id) REFERENCES classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_exams_class_session ON exams (class_id, session_id);

CREATE TABLE exam_subjects (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               exam_id BIGINT NOT NULL,
                               subject_id BIGINT NOT NULL,
                               full_marks DECIMAL(6,2) NOT NULL,
                               pass_marks DECIMAL(6,2) NOT NULL,
                               is_optional BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at DATETIME(6) NOT NULL,
                               updated_at DATETIME(6) NOT NULL,
                               PRIMARY KEY (id),
                               CONSTRAINT uk_exam_subjects UNIQUE (exam_id, subject_id),
                               CONSTRAINT ck_exam_subjects_full CHECK (full_marks > 0),
                               CONSTRAINT ck_exam_subjects_pass CHECK (pass_marks >= 0 AND pass_marks <= full_marks),
                               CONSTRAINT fk_exam_subjects_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
                               CONSTRAINT fk_exam_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;