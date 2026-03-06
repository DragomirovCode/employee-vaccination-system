CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    action VARCHAR(16) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id UUID NOT NULL,
    old_value JSONB NULL,
    new_value JSONB NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users(id)
);
