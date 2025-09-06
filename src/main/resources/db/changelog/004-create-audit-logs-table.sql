--liquibase formatted sql

--changeset openmarket:004-create-audit-logs-table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID, -- user who performed the action
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID,
    payload JSONB,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for actor-based queries
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);

-- Create index for time-based queries
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Create index for entity-based queries
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- Create index for action-based queries
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
