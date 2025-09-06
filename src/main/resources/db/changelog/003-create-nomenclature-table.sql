--liquibase formatted sql

--changeset openmarket:003-create-nomenclature-table
CREATE TABLE nomenclature (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    article TEXT, -- offer_id from Ozon
    sku BIGINT NOT NULL, -- SKU from Ozon
    weight NUMERIC(10,4), -- volume_weight from Ozon
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    note TEXT
);

-- Create unique index for shop_id + sku combination
CREATE UNIQUE INDEX idx_nomenclature_shop_sku ON nomenclature(shop_id, sku);

-- Create index for shop-based queries
CREATE INDEX idx_nomenclature_shop_id ON nomenclature(shop_id);

-- Create index for article lookup
CREATE INDEX idx_nomenclature_article ON nomenclature(article) WHERE article IS NOT NULL;
