CREATE TABLE notification_logs (
                                   id BIGINT NOT NULL AUTO_INCREMENT,
                                   student_id BIGINT NOT NULL,
                                   guardian_id BIGINT NOT NULL,
                                   notification_type VARCHAR(20) NOT NULL,
                                   event_date DATE NOT NULL,
                                   phone VARCHAR(20) NOT NULL,
                                   message VARCHAR(500) NOT NULL,
                                   status VARCHAR(10) NOT NULL,
                                   attempts INT NOT NULL DEFAULT 0,
                                   last_error VARCHAR(255) NULL,
                                   sent_at DATETIME(6) NULL,
                                   created_at DATETIME(6) NOT NULL,
                                   updated_at DATETIME(6) NOT NULL,
                                   PRIMARY KEY (id),
                                   CONSTRAINT uk_notification_once UNIQUE (student_id, notification_type, event_date),
                                   CONSTRAINT ck_notification_type CHECK (notification_type IN ('ABSENCE')),
                                   CONSTRAINT ck_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
                                   CONSTRAINT fk_notification_student FOREIGN KEY (student_id) REFERENCES students (id),
                                   CONSTRAINT fk_notification_guardian FOREIGN KEY (guardian_id) REFERENCES guardians (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_status ON notification_logs (status, id);