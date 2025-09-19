package com.openmarket.repository;

import com.openmarket.dto.counterparty.CreateCounterpartyContractRequest;
import com.openmarket.entity.CounterpartyContract;
import com.openmarket.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CounterpartyContractRepository extends JpaRepository<CounterpartyContract, UUID> {

    default CounterpartyContract saveCounterpartyContract(CreateCounterpartyContractRequest request, Shop shop) {
        CounterpartyContract entity = new CounterpartyContract();
        entity.setShop(shop);
        entity.setCounterparty(request.getCounterparty());
        entity.setContract(request.getContract());
        entity.setContractDate(request.getContractDate());
        return save(entity);
    }

    @Query("SELECT cc FROM CounterpartyContract cc WHERE cc.shop.id = :shopId AND " +
            "(LOWER(cc.counterparty) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(cc.contract) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CounterpartyContract> findByShopIdAndSearch(@Param("shopId") UUID shopId, @Param("search") String search, Pageable pageable);

    List<CounterpartyContract> findByShopId(UUID shopId);

    Page<CounterpartyContract> findByShopId(UUID shopId, Pageable pageable);

    Optional<CounterpartyContract> findByIdAndShopId(UUID id, UUID shopId);

    boolean existsByShopIdAndCounterpartyAndContract(UUID shopId, String counterparty, String contract);
}

