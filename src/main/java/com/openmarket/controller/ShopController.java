package com.openmarket.controller;

import com.openmarket.dto.shop.CreateShopRequest;
import com.openmarket.dto.shop.ShopResponse;
import com.openmarket.dto.shop.UpdateShopRequest;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.ShopService;
import com.openmarket.utils.LogWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/shops")
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    public static final String X_TOTAL_PAGES_HEADER_NAME = "X-Total-Pages";
    public static final String X_TOTAL_COUNT_HEADER_NAME = "X-Total-Count";
    private final ShopService shopService;

    @PostMapping
    public ResponseEntity<ShopResponse> createShop(@Valid @RequestBody CreateShopRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        var shop = LogWrapper.logWrap(
                log,
                "createShop",
                () -> shopService.createShop(request, principal.getUserId())
        );
        return ResponseEntity.ok(shop);
    }

    @GetMapping
    public ResponseEntity<List<ShopResponse>> getUserShops(@AuthenticationPrincipal UserPrincipal principal,
                                                           @RequestParam(defaultValue = "false") boolean paginated,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return LogWrapper.logWrap(
                log,
                "getUserShops",
                () -> {
                    if (paginated) {
                        Page<ShopResponse> shops = shopService.getUserShops(principal.getUserId(), pageable);
                        return ResponseEntity.ok()
                                .header(X_TOTAL_COUNT_HEADER_NAME, String.valueOf(shops.getTotalElements()))
                                .header(X_TOTAL_PAGES_HEADER_NAME, String.valueOf(shops.getTotalPages()))
                                .body(shops.getContent());
                    } else {
                        List<ShopResponse> shops = shopService.getUserShops(principal.getUserId());
                        return ResponseEntity.ok(shops);
                    }
                }
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopResponse> getShop(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return LogWrapper.logWrap(
                log,
                "getShop",
                () -> shopService.getShop(id, principal.getUserId())
                        .map(ResponseEntity::ok)
                        .orElseThrow(() -> new AppNotFoundException("Shop by id %s not found".formatted(id)))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopResponse> updateShop(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateShopRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return LogWrapper.logWrap(
                log,
                "updateShop",
                () -> Optional.ofNullable(shopService.updateShop(id, request, principal.getUserId()))
                        .map(ResponseEntity::ok)
                        .orElseThrow(() -> new AppNotFoundException("Shop by id %s not found".formatted(id)))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShop(@PathVariable UUID id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        LogWrapper.logWrap(
                log,
                "deleteShop",
                () -> {
                    shopService.deleteShop(id, principal.getUserId());
                    return true;
                }
        );
        return ResponseEntity.noContent().build();
    }
}
