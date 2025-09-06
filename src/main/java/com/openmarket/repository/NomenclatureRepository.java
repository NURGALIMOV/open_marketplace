package com.openmarket.repository;

import com.openmarket.entity.Nomenclature;
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
public interface NomenclatureRepository extends JpaRepository<Nomenclature, UUID> {

    Page<Nomenclature> findByShopId(UUID shopId, Pageable pageable);

    @Query("SELECT n FROM Nomenclature n WHERE n.shop.id = :shopId AND " +
           "(LOWER(n.article) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(n.sku AS string) LIKE CONCAT('%', :search, '%'))")
    Page<Nomenclature> findByShopIdAndSearch(@Param("shopId") UUID shopId, 
                                           @Param("search") String search, 
                                           Pageable pageable);

    Optional<Nomenclature> findByShopIdAndSku(UUID shopId, Long sku);
    List<Nomenclature> findByShopId(UUID shopId);

    @Query("SELECT n FROM Nomenclature n WHERE n.shop.id = :shopId AND n.createdAt != n.updatedAt")
    Page<Nomenclature> findUpdatedByShopId(@Param("shopId") UUID shopId, Pageable pageable);

    long countByShopId(UUID shopId);

    @Query("SELECT COUNT(n) FROM Nomenclature n WHERE n.shop.id = :shopId AND n.createdAt != n.updatedAt")
    long countUpdatedByShopId(@Param("shopId") UUID shopId);

    void deleteByShopId(UUID shopId);
}
