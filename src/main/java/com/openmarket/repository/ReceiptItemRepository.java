package com.openmarket.repository;

import com.openmarket.dto.receipt.CreateReceiptItemRequest;
import com.openmarket.entity.Receipt;
import com.openmarket.entity.ReceiptItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, UUID> {

    default ReceiptItem saveReceiptItem(CreateReceiptItemRequest request, Receipt receipt) {
        ReceiptItem entity = new ReceiptItem();
        entity.setReceipt(receipt);
        entity.setSku(request.getSku());
        entity.setArticle(request.getArticle());
        entity.setQuantity(request.getQuantity());
        entity.setCost(request.getCost());
        return save(entity);
    }

    Page<ReceiptItem> findByReceiptId(UUID receiptId, Pageable pageable);

    @Query("SELECT ri FROM ReceiptItem ri WHERE ri.receipt.id = :receiptId AND LOWER(ri.article) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<ReceiptItem> findByReceiptIdAndSearch(@Param("receiptId") UUID receiptId, @Param("search") String search, Pageable pageable);

    Optional<ReceiptItem> findByIdAndReceiptId(UUID id, UUID receiptId);

    @Query("SELECT COALESCE(SUM(ri.quantity * ri.cost), 0) FROM ReceiptItem ri WHERE ri.receipt.id = :receiptId")
    BigDecimal calculateTotalCostByReceiptId(@Param("receiptId") UUID receiptId);

    long countByReceiptId(UUID receiptId);
}

