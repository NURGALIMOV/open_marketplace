--liquibase formatted sql

--changeset openmarket:010-create-shipment-items-table
CREATE TABLE shipment_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    sku BIGINT NOT NULL REFERENCES nomenclature(sku),
    article TEXT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for shipment-based queries
CREATE INDEX idx_shipment_items_shipment_id ON shipment_items(shipment_id);

-- Create index for SKU lookup
CREATE INDEX idx_shipment_items_sku ON shipment_items(sku);

-- Create index for article lookup
CREATE INDEX idx_shipment_items_article ON shipment_items(article) WHERE article IS NOT NULL;

-- Create unique constraint to prevent duplicate shipment_id + sku combinations
CREATE UNIQUE INDEX idx_shipment_items_shipment_sku ON shipment_items(shipment_id, sku);
