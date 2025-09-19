--liquibase formatted sql

--changeset openmarket:007-create-receipts-table
CREATE TABLE receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    request_number TEXT,
    receipt_date DATE,
    counterparty_contract_id UUID REFERENCES counterparties_contracts(id),
    contract TEXT,
    total_cost NUMERIC(18,2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for shop-based queries
CREATE INDEX idx_receipts_shop_id ON receipts(shop_id);

-- Create index for counterparty contract queries
CREATE INDEX idx_receipts_counterparty_contract_id ON receipts(counterparty_contract_id);

-- Create index for date-based queries
CREATE INDEX idx_receipts_receipt_date ON receipts(receipt_date);

-- Create index for request number lookup
CREATE INDEX idx_receipts_request_number ON receipts(request_number) WHERE request_number IS NOT NULL;

