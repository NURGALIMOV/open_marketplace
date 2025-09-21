package com.openmarket.service;

import com.openmarket.dto.shipment.CreateShipmentRequest;
import com.openmarket.dto.shipment.ShipmentResponse;
import com.openmarket.dto.shipment.UpdateShipmentRequest;
import com.openmarket.entity.Shipment;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppAlreadyExistException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.ShipmentItemRepository;
import com.openmarket.repository.ShipmentRepository;
import com.openmarket.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final ShopRepository shopRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Create a new shipment
     */
    @Transactional
    public ShipmentResponse createShipment(UUID shopId, CreateShipmentRequest request, UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        if (shipmentRepository.existsByShopIdAndShipmentNumber(shopId, request.getShipmentNumber())) {
            throw new AppAlreadyExistException("Shipment with number '%s' already exists".formatted(request.getShipmentNumber()));
        }
        Shipment entity = shipmentRepository.saveShipment(request, shop);
        auditLogRepository.saveShipmentAuditLog(request, userId, entity);
        log.info("Shipment created: {} for shop: {}", entity.getShipmentNumber(), shop.getName());
        return ShipmentResponse.from(entity, 0L);
    }

    /**
     * Get shipments for shop
     */
    public Page<ShipmentResponse> getShipments(UUID shopId,
                                               UUID userId,
                                               String search,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               Pageable pageable) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Page<Shipment> entities;
        if (StringUtils.hasText(search)) {
            entities = shipmentRepository.findByShopIdAndSearch(shopId, search, pageable);
        } else if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
            entities = shipmentRepository.findByShopIdAndShipmentDateBetween(shopId, startDate, endDate, pageable);
        } else {
            entities = shipmentRepository.findByShopId(shopId, pageable);
        }
        return entities.map(entity -> {
            long itemsCount = shipmentItemRepository.countByShipmentId(entity.getId());
            return ShipmentResponse.from(entity, itemsCount);
        });
    }

    /**
     * Get shipment by ID
     */
    public ShipmentResponse getShipment(UUID shopId, UUID shipmentId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Shipment entity = shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        long itemsCount = shipmentItemRepository.countByShipmentId(entity.getId());
        return ShipmentResponse.from(entity, itemsCount);
    }

    /**
     * Update shipment
     */
    @Transactional
    public ShipmentResponse updateShipment(UUID shopId, UUID shipmentId, UpdateShipmentRequest request, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Shipment entity = shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        Map<String, Object> changes = new HashMap<>();
        if (StringUtils.hasText(request.getShipmentNumber()) && !request.getShipmentNumber().equals(entity.getShipmentNumber())) {
            if (shipmentRepository.existsByShopIdAndShipmentNumber(shopId, request.getShipmentNumber())) {
                throw new AppAlreadyExistException("Shipment with number '%s' already exists".formatted(request.getShipmentNumber()));
            }
            changes.put("shipmentNumber", Map.of("old", entity.getShipmentNumber(), "new", request.getShipmentNumber()));
            entity.setShipmentNumber(request.getShipmentNumber());
        }
        if (Objects.nonNull(request.getShipmentDate()) && !Objects.equals(request.getShipmentDate(), entity.getShipmentDate())) {
            changes.put("shipmentDate", Map.of("old", entity.getShipmentDate(), "new", request.getShipmentDate()));
            entity.setShipmentDate(request.getShipmentDate());
        }
        if (!changes.isEmpty()) {
            entity = shipmentRepository.save(entity);
            auditLogRepository.saveShipmentAuditLog(userId, entity, "UPDATE_SHIPMENT", changes);
            log.info("Shipment updated: {}", entity.getShipmentNumber());
        }
        long itemsCount = shipmentItemRepository.countByShipmentId(entity.getId());
        return ShipmentResponse.from(entity, itemsCount);
    }

    /**
     * Delete shipment
     */
    @Transactional
    public void deleteShipment(UUID shopId, UUID shipmentId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Shipment entity = shipmentRepository.findByIdAndShopId(shipmentId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        Map<String, Object> payload = Map.of(
                "shipmentNumber", entity.getShipmentNumber(),
                "totalQuantity", entity.getTotalQuantity(),
                "itemsCount", shipmentItemRepository.countByShipmentId(entity.getId())
        );
        auditLogRepository.saveShipmentAuditLog(userId, entity, "DELETE_SHIPMENT", payload);
        shipmentRepository.delete(entity);
        log.info("Shipment deleted: {}", entity.getShipmentNumber());
    }

    /**
     * Recalculate and update total quantity for shipment
     */
    @Transactional
    public void recalculateTotalQuantity(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHIPMENT_NOT_FOUND));
        Long totalQuantity = shipmentItemRepository.calculateTotalQuantityByShipmentId(shipmentId);
        shipment.setTotalQuantity(totalQuantity);
        shipmentRepository.save(shipment);
        log.debug("Recalculated total quantity for shipment {}: {}", shipmentId, totalQuantity);
    }
}
