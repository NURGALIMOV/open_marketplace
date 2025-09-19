--liquibase formatted sql

--changeset openmarket:008-create-receipt-items-table
CREATE TABLE receipt_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id UUID NOT NULL REFERENCES receipts(id) ON DELETE CASCADE,
    sku BIGINT NOT NULL REFERENCES nomenclature(sku),
    article TEXT,
    quantity INTEGER CHECK (quantity >= 0),
    cost NUMERIC(18,2) CHECK (cost >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for receipt-based queries
CREATE INDEX idx_receipt_items_receipt_id ON receipt_items(receipt_id);

-- Create index for SKU lookup
CREATE INDEX idx_receipt_items_sku ON receipt_items(sku) WHERE sku IS NOT NULL;

-- Create index for article lookup
CREATE INDEX idx_receipt_items_article ON receipt_items(article) WHERE article IS NOT NULL;

-- Create unique constraint to prevent duplicate receipt_id + sku combinations
CREATE UNIQUE INDEX idx_receipt_items_receipt_sku ON receipt_items(receipt_id, sku) WHERE sku IS NOT NULL;

