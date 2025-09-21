package com.openmarket.controller;

import com.openmarket.dto.shipment.CreateShipmentItemRequest;
import com.openmarket.dto.shipment.ShipmentItemResponse;
import com.openmarket.dto.shipment.UpdateShipmentItemRequest;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.ShipmentItemService;
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

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops/{shopId}/shipments/{shipmentId}/items")
public class ShipmentItemController {

    private final ShipmentItemService shipmentItemService;

    @PostMapping
    public ResponseEntity<ShipmentItemResponse> createShipmentItem(@PathVariable UUID shopId,
                                                                   @PathVariable UUID shipmentId,
                                                                   @Valid @RequestBody CreateShipmentItemRequest request,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "createShipmentItem",
                () -> shipmentItemService.createShipmentItem(shopId, shipmentId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ShipmentItemResponse>> getShipmentItems(@PathVariable UUID shopId,
                                                                       @PathVariable UUID shipmentId,
                                                                       @RequestParam(required = false) String search,
                                                                       @PageableDefault(size = 50) Pageable pageable,
                                                                       @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getShipmentItems",
                () -> shipmentItemService.getShipmentItems(shopId, shipmentId, principal.getUserId(), search, pageable)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ShipmentItemResponse> getShipmentItem(@PathVariable UUID shopId,
                                                                @PathVariable UUID shipmentId,
                                                                @PathVariable UUID itemId,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getShipmentItem",
                () -> shipmentItemService.getShipmentItem(shopId, shipmentId, itemId, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ShipmentItemResponse> updateShipmentItem(@PathVariable UUID shopId,
                                                                   @PathVariable UUID shipmentId,
                                                                   @PathVariable UUID itemId,
                                                                   @Valid @RequestBody UpdateShipmentItemRequest request,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "updateShipmentItem",
                () -> shipmentItemService.updateShipmentItem(shopId, shipmentId, itemId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteShipmentItem(@PathVariable UUID shopId,
                                                   @PathVariable UUID shipmentId,
                                                   @PathVariable UUID itemId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        LogWrapper.logWrap(
                log,
                "deleteShipmentItem",
                () -> {
                    shipmentItemService.deleteShipmentItem(shopId, shipmentId, itemId, principal.getUserId());
                    return true;
                }
        );
        return ResponseEntity.noContent().build();
    }
}
