package com.openmarket.repository;

import com.openmarket.dto.receipt.CreateReceiptRequest;
import com.openmarket.entity.CounterpartyContract;
import com.openmarket.entity.Receipt;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppBusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    default Receipt saveReceipt(CreateReceiptRequest request, Shop shop, Supplier<CounterpartyContract> supplier) {
        Receipt entity = new Receipt();
        entity.setShop(shop);
        entity.setName(request.getName());
        entity.setRequestNumber(request.getRequestNumber());
        entity.setReceiptDate(request.getReceiptDate());
        if (Objects.nonNull(request.getCounterpartyContractId())) {
            CounterpartyContract counterpartyContract = supplier.get();
            entity.setCounterpartyContract(counterpartyContract);
            entity.setContract(counterpartyContract.getContract());
        } else {
            throw new AppBusinessException("Counterparty contract not found");
        }
        return save(entity);
    }

    Page<Receipt> findByShopId(UUID shopId, Pageable pageable);

    @Query("SELECT r FROM Receipt r WHERE r.shop.id = :shopId AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.requestNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Receipt> findByShopIdAndSearch(@Param("shopId") UUID shopId, @Param("search") String search, Pageable pageable);

    @Query("SELECT r FROM Receipt r WHERE r.shop.id = :shopId AND r.receiptDate BETWEEN :startDate AND :endDate")
    Page<Receipt> findByShopIdAndReceiptDateBetween(@Param("shopId") UUID shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    @Query("SELECT r FROM Receipt r WHERE r.shop.id = :shopId AND r.counterpartyContract.id = :counterpartyContractId")
    Page<Receipt> findByShopIdAndCounterpartyContractId(@Param("shopId") UUID shopId, @Param("counterpartyContractId") UUID counterpartyContractId, Pageable pageable);

    Optional<Receipt> findByIdAndShopId(UUID id, UUID shopId);
}

