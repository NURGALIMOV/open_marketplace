package com.openmarket.service;

import com.openmarket.dto.nomenclature.NomenclatureResponse;
import com.openmarket.dto.ozon.ProductInfo;
import com.openmarket.dto.ozon.ProductListItem;
import com.openmarket.entity.Nomenclature;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppAuthException;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.exception.OzonApiException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.NomenclatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class NomenclatureService {

    private static final String ERROR_KEY = "error";
    private static final String SHOP_NOT_FOUND = "Shop not found";
    private final NomenclatureRepository nomenclatureRepository;
    private final AuditLogRepository auditLogRepository;
    private final ShopService shopService;
    private final OzonApiService ozonApiService;

    /**
     * Get nomenclature for shop with pagination and search
     */
    public Page<NomenclatureResponse> getShopNomenclature(UUID shopId,
                                                          UUID userId,
                                                          String search,
                                                          Pageable pageable) {
        shopService.getShopEntity(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Page<Nomenclature> nomenclaturePage = StringUtils.hasText(search) ?
                nomenclatureRepository.findByShopIdAndSearch(shopId, search, pageable) :
                nomenclatureRepository.findByShopId(shopId, pageable);
        return nomenclaturePage.map(NomenclatureResponse::from);
    }

    /**
     * Get nomenclature statistics for shop
     */
    public Map<String, Object> getShopNomenclatureStats(UUID shopId, UUID userId) {
        shopService.getShopEntity(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        long totalCount = nomenclatureRepository.countByShopId(shopId);
        long updatedCount = nomenclatureRepository.countUpdatedByShopId(shopId);
        return Map.of(
                "totalCount", totalCount,
                "updatedCount", updatedCount,
                "newCount", totalCount - updatedCount
        );
    }

    /**
     * Manual update of nomenclature for shop
     */
    @Transactional
    public Map<String, Object> updateShopNomenclatureManual(UUID shopId, UUID userId) {
        Shop shop = shopService.getShopEntity(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        if (Objects.isNull(shop.getExternalId()) || Objects.isNull(shop.getTokenEncrypted())) {
            throw new AppBusinessException("Shop must have external ID and API token configured");
        }
        String apiKey = Optional.ofNullable(shopService.getDecryptedToken(shop))
                .orElseThrow(() -> new AppBusinessException("Failed to decrypt API token"));
        try {
            Map<String, Object> result = performNomenclatureUpdate(shop, apiKey);
            auditLogRepository.saveNomenclatureAuditLog(userId, "UPDATE_NOMENCLATURE_MANUAL", result, shop);
            return result;
        } catch (OzonApiException e) {
            auditLogRepository.saveNomenclatureAuditLog(userId, "UPDATE_NOMENCLATURE_FAILED", Map.of(ERROR_KEY, e.getMessage()), shop);
            throw new AppBusinessException("Failed to update nomenclature: " + e.getMessage(), e);
        }
    }

    /**
     * System update of nomenclature (called by scheduler)
     */
    @Transactional
    public Map<String, Object> updateShopNomenclatureSystem(Shop shop) {
        String apiKey = shopService.getDecryptedToken(shop);
        if (Objects.isNull(apiKey)) {
            log.error("Failed to decrypt API token for shop: {}", shop.getId());
            return Map.of(ERROR_KEY, "Failed to decrypt API token");
        }
        try {
            Map<String, Object> result = performNomenclatureUpdate(shop, apiKey);
            auditLogRepository.saveNomenclatureAuditLog(null, "UPDATE_NOMENCLATURE_SYSTEM", result, shop);
            return result;
        } catch (Exception e) {
            log.error("System nomenclature update failed for shop: {}", shop.getId(), e);
            Map<String, Object> error = Map.of(ERROR_KEY, e.getMessage());
            auditLogRepository.saveNomenclatureAuditLog(null, "UPDATE_NOMENCLATURE_FAILED", error, shop);
            return error;
        }
    }

    /**
     * Perform the actual nomenclature update
     */
    private Map<String, Object> performNomenclatureUpdate(Shop shop, String apiKey) {
        log.info("Starting nomenclature update for shop: {}", shop.getName());
        OffsetDateTime startTime = OffsetDateTime.now();
        // Step 1: Get product list
        List<ProductListItem> productList =
                ozonApiService.getProductList(shop.getExternalId(), apiKey);
        if (productList.isEmpty()) {
            log.warn("No products found for shop: {}", shop.getName());
            return Map.of(
                    "processed", 0,
                    "new", 0,
                    "updated", 0,
                    "duration", java.time.Duration.between(startTime, OffsetDateTime.now()).toMillis()
            );
        }
        // Step 2: Get detailed product information
        List<Long> productIds = productList.stream().map(ProductListItem::getProductId).toList();
        List<ProductInfo> productInfos = ozonApiService.getProductInfo(shop.getExternalId(), apiKey, productIds);
        // Step 3: Update nomenclature
        int newCount = 0;
        int updatedCount = 0;
        Map<Long, Nomenclature> existNomenclatures = nomenclatureRepository.findByShopId(shop.getId())
                .stream()
                .collect(Collectors.toMap(Nomenclature::getSku, Function.identity()));
        for (ProductInfo productInfo : productInfos) {
            if (Objects.isNull(productInfo.getSku())) {
                log.warn("Product {} has no SKU, skipping", productInfo.getOfferId());
                continue;
            }
            if (existNomenclatures.containsKey(productInfo.getSku())) {
                Nomenclature existNomenclature = existNomenclatures.get(productInfo.getSku());
                if (!existNomenclature.getArticle().equals(productInfo.getOfferId())
                        || existNomenclature.getWeight().doubleValue() != productInfo.getVolumeWeight().doubleValue()) {
                    existNomenclature.setNote("Товар был обновлен, для корректной работы системы необходимо вернуть обратно изменные данные");
                    existNomenclature.setUpdatedAt(OffsetDateTime.now());
                    nomenclatureRepository.save(existNomenclature);
                    updatedCount++;
                    log.info("Updated existing nomenclature item: SKU {}", productInfo.getSku());
                } else {
                    log.info("No changes, skip SKU {}", productInfo.getSku());
                }
            } else {
                Nomenclature nomenclature = new Nomenclature();
                nomenclature.setShop(shop);
                nomenclature.setArticle(productInfo.getOfferId());
                nomenclature.setSku(productInfo.getSku());
                nomenclature.setWeight(productInfo.getVolumeWeight());
                nomenclature.setNote("Товар создан автоматически");
                OffsetDateTime currentTime = OffsetDateTime.now();
                nomenclature.setUpdatedAt(currentTime);
                nomenclature.setCreatedAt(currentTime);
                nomenclatureRepository.save(nomenclature);
                newCount++;
                log.debug("Created new nomenclature item: SKU {}", productInfo.getSku());
            }
        }
        OffsetDateTime endTime = OffsetDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        log.info(
                "Nomenclature update completed for shop: {}. New: {}, Updated: {}, Duration: {}ms",
                shop.getName(),
                newCount,
                updatedCount,
                durationMs
        );
        return Map.of(
                "processed", productInfos.size(),
                "new", newCount,
                "updated", updatedCount,
                "duration", durationMs
        );
    }

    /**
     * Get nomenclature item details
     */
    public NomenclatureResponse getNomenclatureItem(UUID itemId, UUID userId) {
        Nomenclature nomenclature = nomenclatureRepository.findById(itemId).orElseThrow(() -> new AppNotFoundException("Nomenclature item not found"));
        shopService.getShopEntity(nomenclature.getShop().getId(), userId).orElseThrow(() -> new AppAuthException("Access denied"));
        return NomenclatureResponse.from(nomenclature);
    }
}
