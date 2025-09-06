package com.openmarket.controller;

import com.openmarket.dto.nomenclature.NomenclatureResponse;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.NomenclatureService;
import com.openmarket.utils.LogWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops/{shopId}/nomenclature")
public class NomenclatureController {

    private final NomenclatureService nomenclatureService;

    @GetMapping
    public ResponseEntity<Page<NomenclatureResponse>> getShopNomenclature(@PathVariable UUID shopId,
                                                                          @RequestParam(required = false) String search,
                                                                          @PageableDefault(size = 50) Pageable pageable,
                                                                          @AuthenticationPrincipal UserPrincipal principal) {
        var nomenclature = LogWrapper.logWrap(
                log,
                "getShopNomenclature",
                () -> nomenclatureService.getShopNomenclature(shopId, principal.getUserId(), search, pageable)
        );
        return ResponseEntity.ok(nomenclature);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getShopNomenclatureStats(@PathVariable UUID shopId,
                                                                        @AuthenticationPrincipal UserPrincipal principal) {
        var stats = LogWrapper.logWrap(
                log,
                "getShopNomenclatureStats",
                () -> nomenclatureService.getShopNomenclatureStats(shopId, principal.getUserId())
        );
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateShopNomenclature(@PathVariable UUID shopId, @AuthenticationPrincipal UserPrincipal principal) {
        var result = LogWrapper.logWrap(
                log,
                "updateShopNomenclature",
                () -> nomenclatureService.updateShopNomenclatureManual(shopId, principal.getUserId())
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<NomenclatureResponse> getNomenclatureItem(@PathVariable UUID shopId,
                                                                    @PathVariable UUID itemId,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        var item = LogWrapper.logWrap(
                log,
                "getNomenclatureItem",
                () -> nomenclatureService.getNomenclatureItem(itemId, principal.getUserId())
        );
        return ResponseEntity.ok(item);
    }
}
