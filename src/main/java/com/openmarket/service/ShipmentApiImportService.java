package com.openmarket.service;

import com.openmarket.dto.ozon.SupplyOrderBundleItem;
import com.openmarket.dto.ozon.SupplyOrderInfo;
import com.openmarket.dto.shipment.ShipmentApiImportResponse;
import com.openmarket.dto.shipment.ShipmentImportError;
import com.openmarket.entity.Nomenclature;
import com.openmarket.entity.Shipment;
import com.openmarket.entity.ShipmentItem;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.exception.OzonApiException;
import com.openmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentApiImportService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final ShopRepository shopRepository;
    private final AuditLogRepository auditLogRepository;
    private final OzonApiService ozonApiService;
    private final ShipmentService shipmentService;
    private final ShopService shopService;

    /**
     * Import shipments from Ozon API
     */
    @Transactional
    public ShipmentApiImportResponse importShipmentsFromApi(UUID shopId, UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        if (Objects.isNull(shop.getExternalId()) || Objects.isNull(shop.getTokenEncrypted())) {
            throw new AppBusinessException("Shop must have external ID and API token configured");
        }
        String apiKey = getDecryptedToken(shop).filter(StringUtils::hasText)
                .orElseThrow(()  -> new AppBusinessException("Failed to decrypt API token"));
        Map<Long, Nomenclature> shopNomenclature = nomenclatureRepository.findByShopId(shopId).stream().collect(Collectors.toMap(
                Nomenclature::getSku,
                Function.identity()
        ));
        List<ShipmentImportError> errors = new ArrayList<>();
        int shipmentsAdded = 0;
        int shipmentsSkipped = 0;
        try {
            log.info("Starting shipments import from Ozon API for shop: {}", shop.getName());
            List<String> supplyOrderIds = ozonApiService.getSupplyOrderList(shop.getExternalId(), apiKey);
            if (supplyOrderIds.isEmpty()) {
                log.info("No supply orders found for shop: {}", shop.getName());
                return buildResponse(shipmentsAdded, shipmentsSkipped, errors);
            }
            List<SupplyOrderInfo> supplyOrders =
                    ozonApiService.getSupplyOrderDetails(shop.getExternalId(), apiKey, supplyOrderIds);
            for (SupplyOrderInfo supplyOrder : supplyOrders) {
                try {
                    if (processSupplyOrder(shop, supplyOrder, apiKey, shopNomenclature)) {
                        shipmentsAdded++;
                    } else {
                        shipmentsSkipped++;
                    }
                } catch (Exception e) {
                    log.error("Error processing supply order {}: {}", supplyOrder.getSupplyOrderNumber(), e.getMessage());
                    errors.add(ShipmentImportError.builder().shipmentNumber(supplyOrder.getSupplyOrderNumber()).error(e.getMessage()).build());
                    shipmentsSkipped++;
                }
            }
            Map<String, Object> payload = Map.of(
                    "shipmentsAdded", shipmentsAdded,
                    "shipmentsSkipped", shipmentsSkipped,
                    "errorsCount", errors.size(),
                    "totalProcessed", supplyOrders.size()
            );
            auditLogRepository.saveShipmentAuditLog(userId, "IMPORT_SHIPMENTS_FROM_API", payload, shopId);
            log.info("Shipments import completed for shop {}: added={}, skipped={}, errors={}",
                    shop.getName(), shipmentsAdded, shipmentsSkipped, errors.size());
            return buildResponse(shipmentsAdded, shipmentsSkipped, errors);
        } catch (OzonApiException e) {
            log.error("Ozon API error during shipments import for shop {}: {}", shop.getName(), e.getMessage());
            throw new AppBusinessException("Failed to import shipments from Ozon API: " + e.getMessage(), e);
        }
    }

    private boolean processSupplyOrder(Shop shop,
                                       SupplyOrderInfo supplyOrder,
                                       String apiKey,
                                       Map<Long, Nomenclature> shopNomenclature) {
        String shipmentNumber = supplyOrder.getSupplyOrderNumber();
        if (shipmentRepository.existsByShopIdAndShipmentNumber(shop.getId(), shipmentNumber)) {
            log.debug("Shipment {} already exists, skipping", shipmentNumber);
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        Shipment shipment = shipmentRepository.saveShipment(shop, supplyOrder, shipmentNumber, now, now);
        if (Objects.nonNull(supplyOrder.getBundleIdsBySupplyOrderNumber())) {
            processBundleItems(shop, supplyOrder, apiKey, shopNomenclature, shipment);
        }
        log.info("Created shipment from API: {} for shop: {}", shipmentNumber, shop.getName());
        return true;
    }

    private void processBundleItems(Shop shop,
                                    SupplyOrderInfo supplyOrder,
                                    String apiKey,
                                    Map<Long, Nomenclature> shopNomenclature,
                                    Shipment shipment) {
        Set<Long> processedSkus = new HashSet<>();
        List<ShipmentItem> shipmentItems = new ArrayList<>();
        List<String> bundleIds = supplyOrder.getBundleIdsBySupplyOrderNumber().getOrDefault(supplyOrder.getSupplyOrderNumber(), List.of());
        ozonApiService.getSupplyOrderBundleItems(shop.getExternalId(), apiKey, bundleIds).forEach(bundleItem -> {
            if (Objects.isNull(bundleItem.getSku()) || Objects.isNull(bundleItem.getQuantity()) || bundleItem.getQuantity() <= 0) {
                log.warn("Invalid bundle item data for shipment {}: sku={}, quantity={}", shipment.getShipmentNumber(), bundleItem.getSku(), bundleItem.getQuantity());
            } else {
                if (!shopNomenclature.containsKey(bundleItem.getSku())) {
                    throw new AppBusinessException("SKU %s not found in shop nomenclature".formatted(bundleItem.getSku()));
                }
                if (processedSkus.contains(bundleItem.getSku())) {
                    throw new AppBusinessException("Duplicate SKU in shipment: %s".formatted(bundleItem.getSku()));
                }
                processedSkus.add(bundleItem.getSku());
                shipmentItems.add(buildShipmentItem(shipment, bundleItem));
            }
        });
        if (!shipmentItems.isEmpty()) {
            shipmentItemRepository.saveAll(shipmentItems);
            shipmentService.recalculateTotalQuantity(shipment.getId());
        }
    }

    private static ShipmentItem buildShipmentItem(Shipment shipment, SupplyOrderBundleItem bundleItem) {
        ShipmentItem item = new ShipmentItem();
        item.setShipment(shipment);
        item.setSku(bundleItem.getSku());
        item.setArticle(bundleItem.getOfferId());
        item.setQuantity(bundleItem.getQuantity());
        OffsetDateTime now = OffsetDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }


    private Optional<String> getDecryptedToken(Shop shop) {
        try {
            return Optional.ofNullable(shopService.getDecryptedToken(shop));
        } catch (Exception e) {
            log.error("Failed to decrypt token for shop: {}", shop.getId(), e);
            return Optional.empty();
        }
    }

    private ShipmentApiImportResponse buildResponse(int shipmentsAdded,
                                                    int shipmentsSkipped,
                                                    List<ShipmentImportError> errors) {
        return ShipmentApiImportResponse.builder()
                .shipmentsAdded(shipmentsAdded)
                .shipmentsSkipped(shipmentsSkipped)
                .errors(errors)
                .build();
    }
}
