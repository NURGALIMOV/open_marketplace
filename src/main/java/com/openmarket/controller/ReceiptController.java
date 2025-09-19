package com.openmarket.controller;

import com.openmarket.dto.receipt.CreateReceiptRequest;
import com.openmarket.dto.receipt.ReceiptResponse;
import com.openmarket.dto.receipt.UpdateReceiptRequest;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.ReceiptService;
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

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops/{shopId}/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping
    public ResponseEntity<ReceiptResponse> createReceipt(@PathVariable UUID shopId,
                                                         @Valid @RequestBody CreateReceiptRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "createReceipt",
                () -> receiptService.createReceipt(shopId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReceiptResponse>> getReceipts(@PathVariable UUID shopId,
                                                             @RequestParam(required = false) String search,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                             @RequestParam(required = false) UUID counterpartyContractId,
                                                             @PageableDefault(size = 20) Pageable pageable,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getReceipts",
                () -> receiptService.getReceipts(shopId, principal.getUserId(), search, startDate, endDate, counterpartyContractId, pageable)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{receiptId}")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable UUID shopId,
                                                      @PathVariable UUID receiptId,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getReceipt",
                () -> receiptService.getReceipt(shopId, receiptId, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{receiptId}")
    public ResponseEntity<ReceiptResponse> updateReceipt(@PathVariable UUID shopId,
                                                         @PathVariable UUID receiptId,
                                                         @Valid @RequestBody UpdateReceiptRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "updateReceipt",
                () -> receiptService.updateReceipt(shopId, receiptId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{receiptId}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable UUID shopId,
                                              @PathVariable UUID receiptId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        LogWrapper.logWrap(
                log,
                "deleteReceipt",
                () -> {
                    receiptService.deleteReceipt(shopId, receiptId, principal.getUserId());
                    return true;
                }
        );
        return ResponseEntity.noContent().build();
    }
}

