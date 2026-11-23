ALTER TABLE notification_logs 
    ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'BN',
    ADD COLUMN sms_parts INT NOT NULL DEFAULT 1;

CREATE INDEX idx_notif_status_attempts ON notification_logs (status, attempts);
