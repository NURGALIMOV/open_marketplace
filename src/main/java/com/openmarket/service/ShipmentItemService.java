package com.openmarket.service;

import com.openmarket.dto.shipment.CreateShipmentItemRequest;
import com.openmarket.dto.shipment.ShipmentItemResponse;
import com.openmarket.dto.shipment.UpdateShipmentItemRequest;
import com.openmarket.entity.Shipment;
import com.openmarket.entity.ShipmentItem;
import com.openmarket.exception.AppAlreadyExistException;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentItemService {
    
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShopRepository shopRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final AuditLogRepository auditLogRepository;
    private final ShipmentService shipmentService;

    /**
     * Create a new shipment item
     */
    @Transactional
    public ShipmentItemResponse createShipmentItem(UUID shopId, UUID shipmentId, CreateShipmentItemRequest request, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Shipment shipment = shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        nomenclatureRepository.findByShopIdAndSku(shopId, request.getSku())
                .orElseThrow(() -> new AppBusinessException("SKU %s not found in shop nomenclature".formatted(request.getSku())));
        if (shipmentItemRepository.existsByShipmentIdAndSku(shipmentId, request.getSku())) {
            throw new AppAlreadyExistException("Item with SKU %s already exists in this shipment".formatted(request.getSku()));
        }
        ShipmentItem entity = shipmentItemRepository.saveShipmentItem(request, shipment);
        shipmentService.recalculateTotalQuantity(shipmentId);
        auditLogRepository.saveShipmentItemAuditLog(userId, entity);
        log.info("Shipment item created: {} for shipment: {}", entity.getSku(), shipment.getShipmentNumber());
        return ShipmentItemResponse.from(entity);
    }

    /**
     * Get shipment items
     */
    public Page<ShipmentItemResponse> getShipmentItems(UUID shopId, UUID shipmentId, UUID userId, String search, Pageable pageable) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        Page<ShipmentItem> entities = StringUtils.hasText(search) ?
                shipmentItemRepository.findByShipmentIdAndSearch(shipmentId, search, pageable) :
                shipmentItemRepository.findByShipmentId(shipmentId, pageable);
        return entities.map(ShipmentItemResponse::from);
    }

    /**
     * Get shipment item by ID
     */
    public ShipmentItemResponse getShipmentItem(UUID shopId, UUID shipmentId, UUID itemId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        ShipmentItem entity = shipmentItemRepository.findByIdAndShipmentId(itemId, shipmentId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_ITEM_NOT_FOUND));
        return ShipmentItemResponse.from(entity);
    }

    /**
     * Update shipment item
     */
    @Transactional
    public ShipmentItemResponse updateShipmentItem(UUID shopId,
                                                   UUID shipmentId,
                                                   UUID itemId,
                                                   UpdateShipmentItemRequest request,
                                                   UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        ShipmentItem entity = shipmentItemRepository.findByIdAndShipmentId(itemId, shipmentId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_ITEM_NOT_FOUND));
        Map<String, Object> changes = new HashMap<>();
        if (Objects.nonNull(request.getSku()) && !Objects.equals(request.getSku(), entity.getSku())) {
            nomenclatureRepository.findByShopIdAndSku(shopId, request.getSku())
                    .orElseThrow(() -> new AppBusinessException("SKU %s not found in shop nomenclature".formatted(request.getSku())));
            if (shipmentItemRepository.existsByShipmentIdAndSku(shipmentId, request.getSku())) {
                throw new AppAlreadyExistException("Item with SKU %s already exists in this shipment".formatted(request.getSku()));
            }
            changes.put("sku", Map.of("old", entity.getSku(), "new", request.getSku()));
            entity.setSku(request.getSku());
        }
        if (Objects.nonNull(request.getArticle()) && !Objects.equals(request.getArticle(), entity.getArticle())) {
            changes.put("article", Map.of("old", entity.getArticle(), "new", request.getArticle()));
            entity.setArticle(request.getArticle());
        }
        if (Objects.nonNull(request.getQuantity()) && !Objects.equals(request.getQuantity(), entity.getQuantity())) {
            changes.put("quantity", Map.of("old", entity.getQuantity(), "new", request.getQuantity()));
            entity.setQuantity(request.getQuantity());
        }
        if (!changes.isEmpty()) {
            entity = shipmentItemRepository.save(entity);
            shipmentService.recalculateTotalQuantity(shipmentId);
            auditLogRepository.saveShipmentItemAuditLog(userId, "UPDATE_SHIPMENT_ITEM", entity, changes);
            log.info("Shipment item updated: {}", entity.getSku());
        }
        return ShipmentItemResponse.from(entity);
    }

    /**
     * Delete shipment item
     */
    @Transactional
    public void deleteShipmentItem(UUID shopId, UUID shipmentId, UUID itemId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        ShipmentItem entity = shipmentItemRepository.findByIdAndShipmentId(itemId, shipmentId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_ITEM_NOT_FOUND));
        Map<String, Object> payload = Map.of(
                "sku", entity.getSku() != null ? entity.getSku() : "null",
                "article", entity.getArticle() != null ? entity.getArticle() : "null",
                "quantity", entity.getQuantity()
        );
        auditLogRepository.saveShipmentItemAuditLog(userId, "DELETE_SHIPMENT_ITEM", entity, payload);
        shipmentItemRepository.delete(entity);
        shipmentService.recalculateTotalQuantity(shipmentId);
        log.info("Shipment item deleted: {}", entity.getSku());
    }
}
