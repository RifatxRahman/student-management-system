CREATE TABLE academic_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_holiday BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500) NULL,
    target_audience VARCHAR(20) NOT NULL DEFAULT 'ALL',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_academic_event_type CHECK (event_type IN ('EXAM', 'SPORTS', 'CULTURAL', 'MEETING', 'VACATION', 'OBSERVANCE', 'OTHER')),
    CONSTRAINT ck_academic_event_audience CHECK (target_audience IN ('ALL', 'STUDENTS', 'TEACHERS', 'PARENTS')),
    CONSTRAINT ck_academic_event_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_academic_event_session FOREIGN KEY (session_id) REFERENCES academic_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_academic_events_dates ON academic_events (session_id, start_date, end_date);
