package com.openmarket.service;

import com.openmarket.dto.receipt.CreateReceiptRequest;
import com.openmarket.dto.receipt.ReceiptResponse;
import com.openmarket.dto.receipt.UpdateReceiptRequest;
import com.openmarket.entity.CounterpartyContract;
import com.openmarket.entity.Receipt;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final String SHOP_NOT_FOUND = "Shop not found";
    private static final String RECEIPT_NOT_FOUND = "Receipt not found";
    private final ReceiptRepository receiptRepository;
    private final ShopRepository shopRepository;
    private final CounterpartyContractRepository counterpartyContractRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Create a new receipt
     */
    @Transactional
    public ReceiptResponse createReceipt(UUID shopId, CreateReceiptRequest request, UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Supplier<CounterpartyContract> counterpartyContractSupplier =
                () -> counterpartyContractRepository.findByIdAndShopId(request.getCounterpartyContractId(), shopId).orElseThrow(() -> new AppNotFoundException("Counterparty contract not found"));
        Receipt entity = receiptRepository.saveReceipt(request, shop, counterpartyContractSupplier);
        auditLogRepository.saveReceiptAuditLog(request, userId, entity);
        log.info("Receipt created: {} for shop: {}", entity.getName(), shop.getName());
        return ReceiptResponse.from(entity, 0L);
    }

    /**
     * Get receipts for shop
     */
    public Page<ReceiptResponse> getReceipts(UUID shopId, UUID userId, String search, 
                                           LocalDate startDate, LocalDate endDate, 
                                           UUID counterpartyContractId, Pageable pageable) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Page<Receipt> entities;
        if (StringUtils.hasText(search)) {
            entities = receiptRepository.findByShopIdAndSearch(shopId, search, pageable);
        } else if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
            entities = receiptRepository.findByShopIdAndReceiptDateBetween(shopId, startDate, endDate, pageable);
        } else if (Objects.nonNull(counterpartyContractId)) {
            entities = receiptRepository.findByShopIdAndCounterpartyContractId(shopId, counterpartyContractId, pageable);
        } else {
            entities = receiptRepository.findByShopId(shopId, pageable);
        }
        return entities.map(entity -> {
            long itemsCount = receiptItemRepository.countByReceiptId(entity.getId());
            return ReceiptResponse.from(entity, itemsCount);
        });
    }

    /**
     * Get receipt by ID
     */
    public ReceiptResponse getReceipt(UUID shopId, UUID receiptId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Receipt entity = receiptRepository.findByIdAndShopId(receiptId, shopId).orElseThrow(() -> new AppNotFoundException(RECEIPT_NOT_FOUND));
        long itemsCount = receiptItemRepository.countByReceiptId(entity.getId());
        return ReceiptResponse.from(entity, itemsCount);
    }

    /**
     * Update receipt
     */
    @Transactional
    public ReceiptResponse updateReceipt(UUID shopId, UUID receiptId, UpdateReceiptRequest request, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Receipt entity = receiptRepository.findByIdAndShopId(receiptId, shopId).orElseThrow(() -> new AppNotFoundException(RECEIPT_NOT_FOUND));
        Map<String, Object> changes = new HashMap<>();
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(entity.getName())) {
            changes.put("name", Map.of("old", entity.getName(), "new", request.getName()));
            entity.setName(request.getName());
        }
        if (Objects.nonNull(request.getRequestNumber()) && !Objects.equals(request.getRequestNumber(), entity.getRequestNumber())) {
            changes.put("requestNumber", Map.of("old", entity.getRequestNumber(), "new", request.getRequestNumber()));
            entity.setRequestNumber(request.getRequestNumber());
        }
        if (Objects.nonNull(request.getReceiptDate()) && !Objects.equals(request.getReceiptDate(), entity.getReceiptDate())) {
            changes.put("receiptDate", Map.of("old", entity.getReceiptDate(), "new", request.getReceiptDate()));
            entity.setReceiptDate(request.getReceiptDate());
        }
        if (Objects.nonNull(request.getCounterpartyContractId())) {
            UUID currentContractId = Objects.nonNull(entity.getCounterpartyContract()) ? entity.getCounterpartyContract().getId() : null;
            if (!Objects.equals(request.getCounterpartyContractId(), currentContractId)) {
                CounterpartyContract counterpartyContract =
                        counterpartyContractRepository.findByIdAndShopId(request.getCounterpartyContractId(), shopId).orElseThrow(() -> new AppNotFoundException("Counterparty contract not found"));
                changes.put("counterpartyContract", Map.of("old", currentContractId, "new", request.getCounterpartyContractId()));
                entity.setCounterpartyContract(counterpartyContract);
                entity.setContract(counterpartyContract.getContract());
            }
        }
        if (!changes.isEmpty()) {
            entity = receiptRepository.save(entity);
            auditLogRepository.saveReceiptAuditLog(userId, entity, "UPDATE_RECEIPT", changes);
            log.info("Receipt updated: {}", entity.getName());
        }
        long itemsCount = receiptItemRepository.countByReceiptId(entity.getId());
        return ReceiptResponse.from(entity, itemsCount);
    }

    /**
     * Delete receipt
     */
    @Transactional
    public void deleteReceipt(UUID shopId, UUID receiptId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Receipt entity = receiptRepository.findByIdAndShopId(receiptId, shopId).orElseThrow(() -> new AppNotFoundException(RECEIPT_NOT_FOUND));
        Map<String, Object> payload = Map.of(
                "name", entity.getName(),
                "totalCost", entity.getTotalCost(),
                "itemsCount", receiptItemRepository.countByReceiptId(entity.getId())
        );
        auditLogRepository.saveReceiptAuditLog(userId, entity, "DELETE_RECEIPT", payload);
        receiptRepository.delete(entity);
        log.info("Receipt deleted: {}", entity.getName());
    }

    /**
     * Recalculate and update total cost for receipt
     */
    @Transactional
    public void recalculateTotalCost(UUID receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId).orElseThrow(() -> new AppNotFoundException(RECEIPT_NOT_FOUND));
        BigDecimal totalCost = receiptItemRepository.calculateTotalCostByReceiptId(receiptId);
        receipt.setTotalCost(totalCost);
        receiptRepository.save(receipt);
        log.debug("Recalculated total cost for receipt {}: {}", receiptId, totalCost);
    }
}

