CREATE TABLE marks (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       exam_subject_id BIGINT NOT NULL,
                       enrollment_id BIGINT NOT NULL,
                       marks_obtained DECIMAL(6,2) NULL,
                       is_absent BOOLEAN NOT NULL DEFAULT FALSE,
                       entered_by BIGINT NOT NULL,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_marks UNIQUE (exam_subject_id, enrollment_id),
                       CONSTRAINT ck_marks_value CHECK (
                           (is_absent = TRUE AND marks_obtained IS NULL)
                               OR (is_absent = FALSE AND marks_obtained IS NOT NULL AND marks_obtained >= 0)
                           ),
                       CONSTRAINT fk_marks_exam_subject FOREIGN KEY (exam_subject_id) REFERENCES exam_subjects (id),
                       CONSTRAINT fk_marks_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
                       CONSTRAINT fk_marks_entered_by FOREIGN KEY (entered_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_marks_enrollment ON marks (enrollment_id);

CREATE TABLE results (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         exam_id BIGINT NOT NULL,
                         enrollment_id BIGINT NOT NULL,
                         total_marks DECIMAL(8,2) NOT NULL,
                         total_full_marks DECIMAL(8,2) NOT NULL,
                         average_marks DECIMAL(6,2) NOT NULL,
                         gpa DECIMAL(3,2) NOT NULL,
                         grade VARCHAR(10) NOT NULL,
                         is_passed BOOLEAN NOT NULL,
                         position_in_class INT NULL,
                         position_in_section INT NULL,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT uk_results_exam_enrollment UNIQUE (exam_id, enrollment_id),
                         CONSTRAINT ck_results_gpa CHECK (gpa >= 0 AND gpa <= 10),
                         CONSTRAINT ck_results_position CHECK (position_in_class IS NULL OR position_in_class >= 1),
                         CONSTRAINT fk_results_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
                         CONSTRAINT fk_results_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE result_subjects (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 result_id BIGINT NOT NULL,
                                 exam_subject_id BIGINT NOT NULL,
                                 marks_obtained DECIMAL(6,2) NULL,
                                 is_absent BOOLEAN NOT NULL DEFAULT FALSE,
                                 grade VARCHAR(10) NOT NULL,
                                 grade_point DECIMAL(3,2) NOT NULL,
                                 is_passed BOOLEAN NOT NULL,
                                 created_at DATETIME(6) NOT NULL,
                                 updated_at DATETIME(6) NOT NULL,
                                 PRIMARY KEY (id),
                                 CONSTRAINT uk_result_subjects UNIQUE (result_id, exam_subject_id),
                                 CONSTRAINT ck_result_subjects_value CHECK (
                                     (is_absent = TRUE AND marks_obtained IS NULL)
                                         OR (is_absent = FALSE AND marks_obtained IS NOT NULL)
                                     ),
                                 CONSTRAINT fk_result_subjects_result FOREIGN KEY (result_id) REFERENCES results (id),
                                 CONSTRAINT fk_result_subjects_exam_subject FOREIGN KEY (exam_subject_id) REFERENCES exam_subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;