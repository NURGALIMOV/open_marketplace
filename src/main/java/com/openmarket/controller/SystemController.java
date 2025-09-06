package com.openmarket.controller;

import com.openmarket.exception.AppNotFoundException;
import com.openmarket.service.NomenclatureSchedulerService;
import com.openmarket.service.NomenclatureService;
import com.openmarket.service.ShopService;
import com.openmarket.utils.LogWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/system")
@PreAuthorize("hasRole('SYSTEM') or hasRole('ADMIN')")
public class SystemController {

    public static final Map<String, String> SYSTEM_STATUS;
    private final NomenclatureService nomenclatureService;
    private final NomenclatureSchedulerService schedulerService;
    private final ShopService shopService;

    static {
        SYSTEM_STATUS = Map.of(
                "status", "UP",
                "service", "open-market-placer"
        );
    }

    /**
     * Trigger nomenclature update for specific shop (system endpoint)
     */
    @PostMapping("/shops/{shopId}/nomenclature/update")
    public ResponseEntity<Map<String, Object>> updateShopNomenclature(@PathVariable UUID shopId) {
        var result = LogWrapper.logWrap(
                log,
                "updateShopNomenclature",
                () -> {
                    var shopOpt = shopService.getShopEntity(shopId, null)
                            .orElseThrow(() -> new AppNotFoundException("Shop not found by id %s".formatted(shopId)));
                    return nomenclatureService.updateShopNomenclatureSystem(shopOpt);
                }
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Trigger nomenclature update for all shops
     */
    @PostMapping("/nomenclature/update-all")
    public ResponseEntity<Map<String, String>> updateAllShopsNomenclature() {
        LogWrapper.logWrap(
                log,
                "updateAllShopsNomenclature",
                () -> {
                    schedulerService.triggerUpdateAllShops();
                    return true;
                }
        );
        return ResponseEntity.ok(Map.of("message", "Update triggered for all shops"));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(SYSTEM_STATUS);
    }
}
