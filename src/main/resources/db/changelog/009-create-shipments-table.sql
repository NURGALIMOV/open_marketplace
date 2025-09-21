--liquibase formatted sql

--changeset openmarket:009-create-shipments-table
CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    shipment_number TEXT NOT NULL,
    shipment_date DATE NOT NULL,
    total_quantity BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for shop-based queries
CREATE INDEX idx_shipments_shop_id ON shipments(shop_id);

-- Create index for shipment number lookup
CREATE INDEX idx_shipments_number ON shipments(shipment_number);

-- Create index for date-based queries
CREATE INDEX idx_shipments_date ON shipments(shipment_date);

