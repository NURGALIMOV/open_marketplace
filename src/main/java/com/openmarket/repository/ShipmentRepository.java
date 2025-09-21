package com.openmarket.repository;

import com.openmarket.dto.ozon.SupplyOrderInfo;
import com.openmarket.dto.shipment.CreateShipmentRequest;
import com.openmarket.dto.shipment.ShipmentImportRequest;
import com.openmarket.entity.Shipment;
import com.openmarket.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    default Shipment saveShipment(CreateShipmentRequest request, Shop shop) {
        Shipment entity = new Shipment();
        entity.setShop(shop);
        entity.setShipmentNumber(request.getShipmentNumber());
        entity.setShipmentDate(request.getShipmentDate());
        entity.setTotalQuantity(0L);
        return save(entity);
    }

    default Shipment saveShipment(Shop shop, SupplyOrderInfo supplyOrder, String shipmentNumber, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        Shipment shipment = new Shipment();
        shipment.setShop(shop);
        shipment.setShipmentNumber(shipmentNumber);
        shipment.setShipmentDate(convertToLocalDate(supplyOrder.getCreationDate()));
        shipment.setTotalQuantity(0L);
        shipment.setCreatedAt(createdAt);
        shipment.setUpdatedAt(updatedAt);
        return save(shipment);
    }

    private LocalDate convertToLocalDate(OffsetDateTime dateTime) {
        return Objects.nonNull(dateTime) ? dateTime.toLocalDate() : LocalDate.now();
    }

    default Shipment saveShipment(Shop shop, String shipmentNumber, ShipmentImportRequest importRequest) {
        Shipment shipment = new Shipment();
        shipment.setShop(shop);
        shipment.setShipmentNumber(shipmentNumber);
        shipment.setShipmentDate(importRequest.getShipmentDate());
        shipment.setTotalQuantity(0L);
        return save(shipment);
    }

    Page<Shipment> findByShopId(UUID shopId, Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.shop.id = :shopId AND " +
           "(LOWER(s.shipmentNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Shipment> findByShopIdAndSearch(@Param("shopId") UUID shopId, @Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM Shipment s WHERE s.shop.id = :shopId AND s.shipmentDate BETWEEN :startDate AND :endDate")
    Page<Shipment> findByShopIdAndShipmentDateBetween(@Param("shopId") UUID shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    Optional<Shipment> findByIdAndShopId(UUID id, UUID shopId);

    boolean existsByShopIdAndShipmentNumber(UUID shopId, String shipmentNumber);
}
