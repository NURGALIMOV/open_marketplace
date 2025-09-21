package com.openmarket.repository;

import com.openmarket.dto.shipment.CreateShipmentItemRequest;
import com.openmarket.entity.Shipment;
import com.openmarket.entity.ShipmentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, UUID> {

    default ShipmentItem saveShipmentItem(CreateShipmentItemRequest request, Shipment shipment) {
        ShipmentItem entity = new ShipmentItem();
        entity.setShipment(shipment);
        entity.setSku(request.getSku());
        entity.setArticle(request.getArticle());
        entity.setQuantity(request.getQuantity());
        return save(entity);
    }

    Page<ShipmentItem> findByShipmentId(UUID shipmentId, Pageable pageable);

    @Query("SELECT si FROM ShipmentItem si WHERE si.shipment.id = :shipmentId AND " +
           "(LOWER(si.article) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(si.sku AS string) LIKE CONCAT('%', :search, '%'))")
    Page<ShipmentItem> findByShipmentIdAndSearch(@Param("shipmentId") UUID shipmentId, @Param("search") String search, Pageable pageable);

    Optional<ShipmentItem> findByIdAndShipmentId(UUID id, UUID shipmentId);

    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM ShipmentItem si WHERE si.shipment.id = :shipmentId")
    Long calculateTotalQuantityByShipmentId(@Param("shipmentId") UUID shipmentId);

    long countByShipmentId(UUID shipmentId);

    boolean existsByShipmentIdAndSku(UUID shipmentId, Long sku);
}
