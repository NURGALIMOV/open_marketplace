package com.openmarket.controller;

import com.openmarket.dto.shipment.*;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.ShipmentExcelImportService;
import com.openmarket.service.ShipmentService;
import com.openmarket.utils.LogWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops/{shopId}/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentExcelImportService shipmentExcelImportService;


    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(@PathVariable UUID shopId,
                                                           @Valid @RequestBody CreateShipmentRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "createShipment",
                () -> shipmentService.createShipment(shopId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import")
    public ResponseEntity<ShipmentImportResponse> importShipment(@PathVariable UUID shopId,
                                                                 @RequestPart("file") MultipartFile file,
                                                                 @RequestPart("importRequest") ShipmentImportRequest importRequest,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "importShipment",
                () -> shipmentExcelImportService.importShipment(shopId, file, importRequest, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ShipmentResponse>> getShipments(@PathVariable UUID shopId,
                                                               @RequestParam(required = false) String search,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                               @PageableDefault(size = 20) Pageable pageable,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getShipments",
                () -> shipmentService.getShipments(shopId, principal.getUserId(), search, startDate, endDate, pageable)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<ShipmentResponse> getShipment(@PathVariable UUID shopId,
                                                        @PathVariable UUID shipmentId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getShipment",
                () -> shipmentService.getShipment(shopId, shipmentId, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{shipmentId}")
    public ResponseEntity<ShipmentResponse> updateShipment(@PathVariable UUID shopId,
                                                           @PathVariable UUID shipmentId,
                                                           @Valid @RequestBody UpdateShipmentRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "updateShipment",
                () -> shipmentService.updateShipment(shopId, shipmentId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shipmentId}")
    public ResponseEntity<Void> deleteShipment(@PathVariable UUID shopId,
                                               @PathVariable UUID shipmentId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        LogWrapper.logWrap(
                log,
                "deleteShipment",
                () -> {
                    shipmentService.deleteShipment(shopId, shipmentId, principal.getUserId());
                    return true;
                }
        );
        return ResponseEntity.noContent().build();
    }
}
