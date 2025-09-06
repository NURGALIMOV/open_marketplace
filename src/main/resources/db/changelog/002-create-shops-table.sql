--liquibase formatted sql

--changeset openmarket:002-create-shops-table
CREATE TABLE shops (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    external_id TEXT, -- Client-Id for Ozon
    token_encrypted BYTEA, -- Encrypted API-Key
    token_iv BYTEA, -- Initialization vector for encryption
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for user-based queries
CREATE INDEX idx_shops_user_id ON shops(user_id);

-- Create index for external_id lookup
CREATE INDEX idx_shops_external_id ON shops(external_id) WHERE external_id IS NOT NULL;
