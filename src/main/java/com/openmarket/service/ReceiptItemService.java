package com.openmarket.service;

import com.openmarket.dto.receipt.CreateReceiptItemRequest;
import com.openmarket.dto.receipt.ReceiptItemResponse;
import com.openmarket.dto.receipt.UpdateReceiptItemRequest;
import com.openmarket.entity.Receipt;
import com.openmarket.entity.ReceiptItem;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.ReceiptItemRepository;
import com.openmarket.repository.ReceiptRepository;
import com.openmarket.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptItemService {

    private final ReceiptItemRepository receiptItemRepository;
    private final ReceiptRepository receiptRepository;
    private final ShopRepository shopRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReceiptService receiptService;

    /**
     * Create a new receipt item
     */
    @Transactional
    public ReceiptItemResponse createReceiptItem(UUID shopId, UUID receiptId, CreateReceiptItemRequest request, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        Receipt receipt = receiptRepository.findByIdAndShopId(receiptId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_NOT_FOUND));
        ReceiptItem entity = receiptItemRepository.saveReceiptItem(request, receipt);
        receiptService.recalculateTotalCost(receiptId);
        auditLogRepository.saveReceiptItemAuditLog(userId, entity);
        log.info("Receipt item created: {} for receipt: {}", entity.getSku(), receipt.getName());
        return ReceiptItemResponse.from(entity);
    }

    /**
     * Get receipt items
     */
    public Page<ReceiptItemResponse> getReceiptItems(UUID shopId, UUID receiptId, UUID userId, String search, Pageable pageable) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        receiptRepository.findByIdAndShopId(receiptId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_NOT_FOUND));
        Page<ReceiptItem> entities = StringUtils.hasText(search) ?
                receiptItemRepository.findByReceiptIdAndSearch(receiptId, search, pageable) :
                receiptItemRepository.findByReceiptId(receiptId, pageable);
        return entities.map(ReceiptItemResponse::from);
    }

    /**
     * Get receipt item by ID
     */
    public ReceiptItemResponse getReceiptItem(UUID shopId, UUID receiptId, UUID itemId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        receiptRepository.findByIdAndShopId(receiptId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_NOT_FOUND));
        ReceiptItem entity = receiptItemRepository.findByIdAndReceiptId(itemId, receiptId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_ITEM_NOT_FOUND));
        return ReceiptItemResponse.from(entity);
    }

    /**
     * Update receipt item
     */
    @Transactional
    public ReceiptItemResponse updateReceiptItem(UUID shopId,
                                                 UUID receiptId,
                                                 UUID itemId,
                                                 UpdateReceiptItemRequest request,
                                                 UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        receiptRepository.findByIdAndShopId(receiptId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_NOT_FOUND));
        ReceiptItem entity = receiptItemRepository.findByIdAndReceiptId(itemId, receiptId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_ITEM_NOT_FOUND));
        Map<String, Object> changes = new HashMap<>();
        if (Objects.nonNull(request.getSku()) && !Objects.equals(request.getSku(), entity.getSku())) {
            changes.put("sku", Map.of("old", entity.getSku(), "new", request.getSku()));
            entity.setSku(request.getSku());
        }
        if (Objects.nonNull(request.getArticle()) && !Objects.equals(request.getArticle(), entity.getArticle())) {
            changes.put("article", Map.of("old", entity.getArticle(), "new", request.getArticle()));
            entity.setArticle(request.getArticle());
        }
        if (Objects.nonNull(request.getQuantity()) && !Objects.equals(request.getQuantity(), entity.getQuantity())) {
            changes.put("quantity", Map.of("old", entity.getQuantity(), "new", request.getQuantity()));
            entity.setQuantity(request.getQuantity());
        }
        if (Objects.nonNull(request.getCost()) && !Objects.equals(request.getCost(), entity.getCost())) {
            changes.put("cost", Map.of("old", entity.getCost(), "new", request.getCost()));
            entity.setCost(request.getCost());
        }
        if (!changes.isEmpty()) {
            entity = receiptItemRepository.save(entity);
            receiptService.recalculateTotalCost(receiptId);
            auditLogRepository.saveReceiptItemAuditLog(userId, "UPDATE_RECEIPT_ITEM", entity, changes);
            log.info("Receipt item updated: {}", entity.getSku());
        }
        return ReceiptItemResponse.from(entity);
    }



    /**
     * Delete receipt item
     */
    @Transactional
    public void deleteReceiptItem(UUID shopId, UUID receiptId, UUID itemId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(AppNotFoundException.SHOP_NOT_FOUND));
        receiptRepository.findByIdAndShopId(receiptId, shopId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_NOT_FOUND));
        ReceiptItem entity = receiptItemRepository.findByIdAndReceiptId(itemId, receiptId)
                .orElseThrow(() -> new AppNotFoundException(AppNotFoundException.RECEIPT_ITEM_NOT_FOUND));
        Map<String, Object> payload = Map.of(
                "sku", entity.getSku() != null ? entity.getSku() : "null",
                "article", entity.getArticle() != null ? entity.getArticle() : "null",
                "quantity", entity.getQuantity(),
                "cost", entity.getCost(),
                "totalCost", entity.getTotalCost()
        );
        auditLogRepository.saveReceiptItemAuditLog(userId, "DELETE_RECEIPT_ITEM", entity, payload);
        receiptItemRepository.delete(entity);
        receiptService.recalculateTotalCost(receiptId);
        log.info("Receipt item deleted: {}", entity.getSku());
    }
}

