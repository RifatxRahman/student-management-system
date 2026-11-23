-- V20__enhance_audit_logs_and_security.sql
-- সিস্টেম অডিট লগ সম্প্রসারণ ও উচ্চ পারফরম্যান্স ইনডেক্সিং

ALTER TABLE audit_logs
    ADD COLUMN ip_address VARCHAR(45) NULL AFTER details,
    ADD COLUMN user_agent VARCHAR(255) NULL AFTER ip_address,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' AFTER user_agent,
    ADD COLUMN execution_time_ms BIGINT NOT NULL DEFAULT 0 AFTER status;

-- অডিট ফিল্টারিং ও সার্চের গতি বাড়ানোর জন্য ইনডেক্স
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_status ON audit_logs (status);
