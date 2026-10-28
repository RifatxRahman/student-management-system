CREATE TABLE academic_sessions (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   name VARCHAR(50) NOT NULL,
                                   start_date DATE NOT NULL,
                                   end_date DATE NOT NULL,
                                   is_active BOOLEAN NOT NULL DEFAULT FALSE,
                                   active_key TINYINT GENERATED ALWAYS AS (IF(is_active, 1, NULL)) STORED,
                                   created_at DATETIME(6) NOT NULL,
                                   updated_at DATETIME(6) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT uk_academic_sessions_name UNIQUE (name),
                                   CONSTRAINT uk_academic_sessions_active UNIQUE (active_key),
                                   CONSTRAINT ck_academic_sessions_dates CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classes (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         name VARCHAR(50) NOT NULL,
                         display_order INT NOT NULL DEFAULT 0,
                         is_active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT uk_classes_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sections (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          class_id BIGINT NOT NULL,
                          name VARCHAR(20) NOT NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_sections_class_name UNIQUE (class_id, name),
                          CONSTRAINT fk_sections_class FOREIGN KEY (class_id) REFERENCES classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subjects (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          name VARCHAR(100) NOT NULL,
                          code VARCHAR(20) NOT NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6) NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_subjects_name UNIQUE (name),
                          CONSTRAINT uk_subjects_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_subjects (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                class_id BIGINT NOT NULL,
                                subject_id BIGINT NOT NULL,
                                created_at DATETIME(6) NOT NULL,
                                updated_at DATETIME(6) NOT NULL,
                                PRIMARY KEY (id),
                                CONSTRAINT uk_class_subjects UNIQUE (class_id, subject_id),
                                CONSTRAINT fk_class_subjects_class FOREIGN KEY (class_id) REFERENCES classes (id),
                                CONSTRAINT fk_class_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;