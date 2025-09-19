--liquibase formatted sql

--changeset openmarket:006-create-counterparties-contracts-table
CREATE TABLE counterparties_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    counterparty TEXT NOT NULL,
    contract TEXT NOT NULL,
    contract_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for shop-based queries
CREATE INDEX idx_counterparties_contracts_shop_id ON counterparties_contracts(shop_id);

-- Create index for search by counterparty
CREATE INDEX idx_counterparties_contracts_counterparty ON counterparties_contracts(counterparty);

-- Create index for search by contract
CREATE INDEX idx_counterparties_contracts_contract ON counterparties_contracts(contract);

