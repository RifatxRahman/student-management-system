ALTER TABLE teachers
    ADD COLUMN employee_id VARCHAR(30) NULL AFTER id,
    ADD COLUMN email VARCHAR(100) NULL AFTER phone,
    ADD COLUMN qualification VARCHAR(150) NULL AFTER designation,
    ADD COLUMN joining_date DATE NULL AFTER qualification,
    ADD COLUMN photo_path VARCHAR(120) NULL AFTER joining_date,
    ADD COLUMN address VARCHAR(200) NULL AFTER photo_path;

ALTER TABLE teachers
    ADD CONSTRAINT uk_teachers_employee_id UNIQUE (employee_id);
