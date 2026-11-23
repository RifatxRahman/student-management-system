CREATE TABLE teacher_attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(15) NOT NULL,
    in_time TIME NULL,
    out_time TIME NULL,
    remark VARCHAR(255) NULL,
    marked_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_attendance_date UNIQUE (teacher_id, attendance_date),
    CONSTRAINT ck_teacher_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'LEAVE', 'HALF_DAY')),
    CONSTRAINT fk_teacher_attendance_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_teacher_attendance_marked_by FOREIGN KEY (marked_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_teacher_attendance_date ON teacher_attendance (attendance_date);

CREATE TABLE leave_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(4, 1) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    applied_at DATETIME(6) NOT NULL,
    action_by BIGINT NULL,
    action_at DATETIME(6) NULL,
    action_reason VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_leave_type CHECK (leave_type IN ('CASUAL', 'SICK', 'EARNED', 'MATERNITY', 'UNPAID')),
    CONSTRAINT ck_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT fk_leave_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_leave_action_by FOREIGN KEY (action_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_leave_teacher_dates ON leave_applications (teacher_id, start_date, end_date);
CREATE INDEX idx_leave_status ON leave_applications (status);
