--liquibase formatted sql

--changeset openmarket:005-create-additional-indexes
-- Additional performance indexes

-- Index for updated nomenclature items (for UI highlighting)
CREATE INDEX idx_nomenclature_updated ON nomenclature(shop_id, updated_at) 
WHERE created_at != updated_at;

-- Partial index for active shops (with tokens)
CREATE INDEX idx_shops_active ON shops(user_id, id) 
WHERE token_encrypted IS NOT NULL;

-- Index for recent audit logs (performance optimization)
CREATE INDEX idx_audit_logs_recent ON audit_logs(created_at DESC, actor_id);
