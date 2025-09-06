package com.openmarket.service;

import com.openmarket.entity.Shop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for scheduled nomenclature updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NomenclatureSchedulerService {

    private final ShopService shopService;
    private final NomenclatureService nomenclatureService;

    /**
     * Scheduled method to update nomenclature for all shops. Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void updateAllShopsNomenclature() {
        log.info("Starting scheduled nomenclature update for all shops");
        List<Shop> shopsWithTokens = shopService.getAllShopsWithTokens();
        if (shopsWithTokens.isEmpty()) {
            log.info("No shops with API tokens found for scheduled update");
            return;
        }
        log.info("Found {} shops with API tokens for scheduled update", shopsWithTokens.size());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        shopsWithTokens.forEach(shop -> updateAllShopsNomenclature(shop, successCount, failureCount));
        log.info("Scheduled nomenclature update completed. Success: {}, Failures: {}", successCount.get(), failureCount.get());
    }

    private void updateAllShopsNomenclature(Shop shop, AtomicInteger successCount, AtomicInteger failureCount) {
        try {
            Map<String, Object> result = nomenclatureService.updateShopNomenclatureSystem(shop);
            if (result.containsKey("error")) {
                log.error("Scheduled update failed for shop {}: {}", shop.getName(), result.get("error"));
                failureCount.incrementAndGet();
                return;
            }
            log.info("Scheduled update completed for shop {}: processed={}, new={}, updated={}",
                    shop.getName(),
                    result.get("processed"),
                    result.get("new"),
                    result.get("updated"));
            successCount.incrementAndGet();
        } catch (Exception e) {
            log.error("Unexpected error during scheduled update for shop {}: {}", shop.getName(), e.getMessage(), e);
            failureCount.incrementAndGet();
        }
    }

    /**
     * Manual trigger for updating all shops (for admin use)
     */
    public void triggerUpdateAllShops() {
        log.info("Manual trigger for updating all shops nomenclature");
        updateAllShopsNomenclature();
    }
}
