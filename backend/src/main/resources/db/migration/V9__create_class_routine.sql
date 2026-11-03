CREATE TABLE rooms (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL,
                       capacity INT NULL,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_rooms_name UNIQUE (name),
                       CONSTRAINT ck_rooms_capacity CHECK (capacity IS NULL OR capacity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE time_slots (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            name VARCHAR(50) NOT NULL,
                            start_time TIME NOT NULL,
                            end_time TIME NOT NULL,
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT uk_time_slots_name UNIQUE (name),
                            CONSTRAINT uk_time_slots_range UNIQUE (start_time, end_time),
                            CONSTRAINT ck_time_slots_range CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE timetable (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           session_id BIGINT NOT NULL,
                           section_id BIGINT NOT NULL,
                           day_of_week VARCHAR(10) NOT NULL,
                           time_slot_id BIGINT NOT NULL,
                           subject_id BIGINT NOT NULL,
                           teacher_id BIGINT NOT NULL,
                           room_id BIGINT NULL,
                           created_at DATETIME(6) NOT NULL,
                           updated_at DATETIME(6) NOT NULL,
                           PRIMARY KEY (id),
                           CONSTRAINT uk_timetable_section_slot UNIQUE (session_id, section_id, day_of_week, time_slot_id),
                           CONSTRAINT uk_timetable_teacher_slot UNIQUE (session_id, teacher_id, day_of_week, time_slot_id),
                           CONSTRAINT uk_timetable_room_slot UNIQUE (session_id, room_id, day_of_week, time_slot_id),
                           CONSTRAINT ck_timetable_day CHECK (day_of_week IN
                                                              ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
                           CONSTRAINT fk_timetable_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id),
                           CONSTRAINT fk_timetable_section FOREIGN KEY (section_id) REFERENCES sections (id),
                           CONSTRAINT fk_timetable_slot FOREIGN KEY (time_slot_id) REFERENCES time_slots (id),
                           CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
                           CONSTRAINT fk_timetable_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
                           CONSTRAINT fk_timetable_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_timetable_teacher ON timetable (teacher_id, session_id);