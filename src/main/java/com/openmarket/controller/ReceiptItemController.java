package com.openmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openmarket.dto.receipt.*;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.ReceiptItemsExcelImportService;
import com.openmarket.service.ReceiptItemService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops/{shopId}/receipts/{receiptId}/items")
public class ReceiptItemController {

    private final ReceiptItemService receiptItemService;
    private final ReceiptItemsExcelImportService receiptItemsExcelImportService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ReceiptItemResponse> createReceiptItem(@PathVariable UUID shopId,
                                                                 @PathVariable UUID receiptId,
                                                                 @Valid @RequestBody CreateReceiptItemRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "createReceiptItem",
                () -> receiptItemService.createReceiptItem(shopId, receiptId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReceiptItemResponse>> getReceiptItems(@PathVariable UUID shopId,
                                                                     @PathVariable UUID receiptId,
                                                                     @RequestParam(required = false) String search,
                                                                     @PageableDefault(size = 50) Pageable pageable,
                                                                     @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getReceiptItems",
                () -> receiptItemService.getReceiptItems(shopId, receiptId, principal.getUserId(), search, pageable)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ReceiptItemResponse> getReceiptItem(@PathVariable UUID shopId,
                                                              @PathVariable UUID receiptId,
                                                              @PathVariable UUID itemId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getReceiptItem",
                () -> receiptItemService.getReceiptItem(shopId, receiptId, itemId, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ReceiptItemResponse> updateReceiptItem(@PathVariable UUID shopId,
                                                                 @PathVariable UUID receiptId,
                                                                 @PathVariable UUID itemId,
                                                                 @Valid @RequestBody UpdateReceiptItemRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "updateReceiptItem",
                () -> receiptItemService.updateReceiptItem(shopId, receiptId, itemId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteReceiptItem(@PathVariable UUID shopId,
                                                  @PathVariable UUID receiptId,
                                                  @PathVariable UUID itemId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        LogWrapper.logWrap(
                log,
                "deleteReceiptItem",
                () -> {
                    receiptItemService.deleteReceiptItem(shopId, receiptId, itemId, principal.getUserId());
                    return true;
                }
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<ExcelImportResponse> importReceiptItems(@PathVariable UUID shopId,
                                                                  @PathVariable UUID receiptId,
                                                                  @RequestParam("file") MultipartFile file,
                                                                  @RequestParam("mapping") String mappingJson,
                                                                  @RequestParam(value = "strict", defaultValue = "true") boolean strict,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "importReceiptItems",
                () -> {
                    try {
                        ExcelImportRequest importRequest = objectMapper.readValue(mappingJson, ExcelImportRequest.class);
                        importRequest.setStrict(strict);
                        return receiptItemsExcelImportService.importReceiptItems(shopId, receiptId, file, importRequest, principal.getUserId());
                    } catch (Exception e) {
                        throw new AppBusinessException("Failed to parse import request: %s".formatted(e.getMessage()), e);
                    }
                }
        );
        return ResponseEntity.ok(response);
    }
}

