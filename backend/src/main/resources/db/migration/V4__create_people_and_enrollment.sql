CREATE TABLE teachers (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          user_id BIGINT NULL,
                          full_name VARCHAR(100) NOT NULL,
                          phone VARCHAR(20) NULL,
                          designation VARCHAR(100) NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_teachers_user UNIQUE (user_id),
                          CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teacher_assignments (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     teacher_id BIGINT NOT NULL,
                                     session_id BIGINT NOT NULL,
                                     section_id BIGINT NOT NULL,
                                     subject_id BIGINT NOT NULL,
                                     created_at DATETIME(6) NOT NULL,
                                     updated_at DATETIME(6) NOT NULL,
                                     PRIMARY KEY (id),
                                     CONSTRAINT uk_teacher_assignments UNIQUE (session_id, section_id, subject_id),
                                     CONSTRAINT fk_ta_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
                                     CONSTRAINT fk_ta_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
                                     CONSTRAINT fk_ta_section FOREIGN KEY (section_id) REFERENCES sections (id),
                                     CONSTRAINT fk_ta_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ta_teacher ON teacher_assignments (teacher_id);

CREATE TABLE guardians (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           full_name VARCHAR(100) NOT NULL,
                           phone VARCHAR(20) NOT NULL,
                           alt_phone VARCHAR(20) NULL,
                           email VARCHAR(100) NULL,
                           address VARCHAR(255) NULL,
                           created_at DATETIME(6) NOT NULL,
                           updated_at DATETIME(6) NOT NULL,
                           PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_guardians_phone ON guardians (phone);

CREATE TABLE students (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          student_code VARCHAR(20) NOT NULL,
                          full_name VARCHAR(100) NOT NULL,
                          date_of_birth DATE NOT NULL,
                          gender VARCHAR(10) NOT NULL,
                          phone VARCHAR(20) NULL,
                          address VARCHAR(255) NULL,
                          admission_date DATE NOT NULL,
                          photo_path VARCHAR(255) NULL,
                          status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                          deleted_at DATETIME(6) NULL,
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_students_code UNIQUE (student_code),
                          CONSTRAINT ck_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
                          CONSTRAINT ck_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'GRADUATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_students_status ON students (status);
CREATE INDEX idx_students_name ON students (full_name);

CREATE TABLE student_guardians (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   student_id BIGINT NOT NULL,
                                   guardian_id BIGINT NOT NULL,
                                   relation VARCHAR(20) NOT NULL,
                                   is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                   primary_student_key BIGINT GENERATED ALWAYS AS (IF(is_primary, student_id, NULL)) STORED,
                                   created_at DATETIME(6) NOT NULL,
                                   updated_at DATETIME(6) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT uk_student_guardians UNIQUE (student_id, guardian_id),
                                   CONSTRAINT uk_student_guardians_primary UNIQUE (primary_student_key),
                                   CONSTRAINT ck_student_guardians_relation CHECK (relation IN ('FATHER', 'MOTHER', 'GUARDIAN', 'OTHER')),
                                   CONSTRAINT fk_sg_student FOREIGN KEY (student_id) REFERENCES students (id),
                                   CONSTRAINT fk_sg_guardian FOREIGN KEY (guardian_id) REFERENCES guardians (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sg_guardian ON student_guardians (guardian_id);

CREATE TABLE enrollments (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             student_id BIGINT NOT NULL,
                             session_id BIGINT NOT NULL,
                             class_id BIGINT NOT NULL,
                             section_id BIGINT NOT NULL,
                             roll_number INT NOT NULL,
                             status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                             start_date DATE NOT NULL,
                             end_date DATE NULL,
                             active_student_key VARCHAR(40)
                                 GENERATED ALWAYS AS (IF(status = 'ACTIVE', CONCAT(student_id, ':', session_id), NULL)) STORED,
                             active_roll_key VARCHAR(60)
                                 GENERATED ALWAYS AS (IF(status = 'ACTIVE', CONCAT(session_id, ':', section_id, ':', roll_number), NULL)) STORED,
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             PRIMARY KEY (id),
                             CONSTRAINT uk_enrollments_active_student UNIQUE (active_student_key),
                             CONSTRAINT uk_enrollments_active_roll UNIQUE (active_roll_key),
                             CONSTRAINT ck_enrollments_roll CHECK (roll_number > 0),
                             CONSTRAINT ck_enrollments_dates CHECK (end_date IS NULL OR end_date >= start_date),
                             CONSTRAINT ck_enrollments_status CHECK (status IN ('ACTIVE', 'TRANSFERRED', 'WITHDRAWN', 'COMPLETED')),
                             CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
                             CONSTRAINT fk_enrollments_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
                             CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id) REFERENCES classes (id),
                             CONSTRAINT fk_enrollments_section FOREIGN KEY (section_id) REFERENCES sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_enrollments_student ON enrollments (student_id);
CREATE INDEX idx_enrollments_section ON enrollments (session_id, section_id);

CREATE TABLE id_sequences (
                              name VARCHAR(50) NOT NULL,
                              counter_value BIGINT NOT NULL,
                              PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            actor_user_id BIGINT NULL,
                            action VARCHAR(50) NOT NULL,
                            entity_type VARCHAR(50) NOT NULL,
                            entity_id BIGINT NULL,
                            details VARCHAR(1000) NULL,
                            created_at DATETIME(6) NOT NULL,
                            PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id, created_at);
CREATE INDEX idx_audit_actor ON audit_logs (actor_user_id, created_at);