package com.openmarket.repository;

import com.openmarket.dto.receipt.CreateReceiptRequest;
import com.openmarket.dto.shipment.CreateShipmentRequest;
import com.openmarket.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    default void saveShopAuditLog(UUID userId, String action, Shop shop) {
        AuditLog auditLog = AuditLog.create(userId, action, "SHOP", shop.getId());
        auditLog.setPayload(Map.of(
                "name", shop.getName(),
                "externalId", Objects.nonNull(shop.getExternalId()) ? shop.getExternalId() : "",
                "hasToken", Objects.nonNull(shop.getTokenEncrypted())
        ));
        save(auditLog);
    }

    default void saveShopAuditLog(UUID userId, Map<String, Object> changes, Shop shop) {
        AuditLog auditLog = AuditLog.create(userId, "UPDATE_SHOP", "SHOP", shop.getId());
        auditLog.setPayload(changes);
        save(auditLog);
    }

    default void saveNomenclatureAuditLog(UUID userId, String action, Map<String, Object> changes, Shop shop) {
        AuditLog auditLog = AuditLog.create(userId, action, "SHOP", shop.getId());
        auditLog.setPayload(changes);
        save(auditLog);
    }

    default void saveUserAuditLog(UUID userId, String action, Map<String, Object> changes) {
        AuditLog auditLog = AuditLog.create(userId, action, "USER", userId);
        auditLog.setPayload(changes);
        save(auditLog);
    }

    default void saveUserAuditLog(UUID actorId, String action, Map<String, Object> changes, User user) {
        AuditLog auditLog = AuditLog.create(actorId, action, "USER", user.getId());
        auditLog.setPayload(changes);
        save(auditLog);
    }

    default void saveCounterpartyContractAuditLog(CounterpartyContract entity, UUID userId, String action) {
        Map<String, Object> payload = Map.of(
                "counterparty", entity.getCounterparty(),
                "contract", entity.getContract()
        );
        AuditLog counterpartyContract = AuditLog.create(
                userId,
                action,
                "COUNTERPARTY_CONTRACT",
                entity.getId(),
                payload
        );
        save(counterpartyContract);
    }

    default void saveCounterpartyContractAuditLog(UUID userId, CounterpartyContract entity, Map<String, Object> changes) {
        save(AuditLog.create(userId, "UPDATE_COUNTERPARTY_CONTRACT", "COUNTERPARTY_CONTRACT", entity.getId(), changes));
    }

    default void saveReceiptItemAuditLog(UUID userId, ReceiptItem entity) {
        Map<String, Object> payload = Map.of(
                "sku", entity.getSku() != null ? entity.getSku() : "null",
                "article", entity.getArticle() != null ? entity.getArticle() : "null",
                "quantity", entity.getQuantity(),
                "cost", entity.getCost(),
                "totalCost", entity.getTotalCost()
        );
        AuditLog auditLog = AuditLog.create(
                userId,
                "CREATE_RECEIPT_ITEM",
                "RECEIPT_ITEM",
                entity.getId(),
                payload
        );
        save(auditLog);
    }

    default void saveReceiptItemAuditLog(UUID userId, String action, ReceiptItem entity, Map<String, Object> changes) {
        AuditLog auditLog = AuditLog.create(
                userId,
                action,
                "RECEIPT_ITEM",
                entity.getId(),
                changes
        );
        save(auditLog);
    }

    default void saveReceiptAuditLog(CreateReceiptRequest request, UUID userId, Receipt entity) {
        Map<String, Object> payload = Map.of(
                "name", entity.getName(),
                "counterpartyContractId", request.getCounterpartyContractId() != null ? request.getCounterpartyContractId().toString() : "null"
        );
        AuditLog auditLog = AuditLog.create(
                userId,
                "CREATE_RECEIPT",
                "RECEIPT",
                entity.getId(),
                payload
        );
        save(auditLog);
    }

    default void saveReceiptAuditLog(UUID userId, Receipt entity, String action, Map<String, Object> changes) {
        AuditLog auditLog = AuditLog.create(
                userId,
                action,
                "RECEIPT",
                entity.getId(),
                changes
        );
        save(auditLog);
    }

    default void saveShipmentAuditLog(CreateShipmentRequest request, UUID userId, Shipment entity) {
        Map<String, Object> payload = Map.of(
                "shipmentNumber", entity.getShipmentNumber(),
                "shipmentDate", entity.getShipmentDate().toString()
        );
        AuditLog auditLog = AuditLog.create(
                userId,
                "CREATE_SHIPMENT",
                "SHIPMENT",
                entity.getId(),
                payload
        );
        save(auditLog);
    }

    default void saveShipmentAuditLog(UUID userId, Shipment entity, String action, Map<String, Object> changes) {
        AuditLog auditLog = AuditLog.create(
                userId,
                action,
                "SHIPMENT",
                entity.getId(),
                changes
        );
        save(auditLog);
    }

    default void saveShipmentItemAuditLog(UUID userId, ShipmentItem entity) {
        Map<String, Object> payload = Map.of(
                "sku", entity.getSku() != null ? entity.getSku() : "null",
                "article", entity.getArticle() != null ? entity.getArticle() : "null",
                "quantity", entity.getQuantity()
        );
        AuditLog auditLog = AuditLog.create(
                userId,
                "CREATE_SHIPMENT_ITEM",
                "SHIPMENT_ITEM",
                entity.getId(),
                payload
        );
        save(auditLog);
    }

    default void saveShipmentItemAuditLog(UUID userId, String action, ShipmentItem entity, Map<String, Object> changes) {
        AuditLog auditLog = AuditLog.create(
                userId,
                action,
                "SHIPMENT_ITEM",
                entity.getId(),
                changes
        );
        save(auditLog);
    }

}
