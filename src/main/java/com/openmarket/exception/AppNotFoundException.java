package com.openmarket.exception;

public class AppNotFoundException extends RuntimeException {

    public static final String SHOP_NOT_FOUND = "Shop not found";
    public static final String SHIPMENT_NOT_FOUND = "Shipment not found";
    public static final String RECEIPT_NOT_FOUND = "Receipt not found";
    public static final String RECEIPT_ITEM_NOT_FOUND = "Receipt item not found";
    public static final String SHIPMENT_ITEM_NOT_FOUND = "Shipment item not found";
    public static final String COUNTERPARTY_CONTRACT_NOT_FOUND = "Counterparty contract not found";

    public AppNotFoundException(String message) {
        super(message);
    }
}
