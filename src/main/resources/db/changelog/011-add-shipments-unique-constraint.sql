--liquibase formatted sql

--changeset openmarket:011-add-shipments-unique-constraint
-- Add unique constraint for shop_id + shipment_number to prevent duplicates
CREATE UNIQUE INDEX idx_shipments_shop_number ON shipments(shop_id, shipment_number);
