package com.openmarket.repository;

import com.openmarket.dto.auth.EncryptionResult;
import com.openmarket.dto.shop.CreateShopRequest;
import com.openmarket.entity.Shop;
import com.openmarket.entity.User;
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
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    default Shop saveShop(User user, CreateShopRequest request, EncryptionResult encryptionResult) {
        Shop shop = new Shop();
        shop.setUser(user);
        shop.setName(request.getName());
        shop.setExternalId(request.getExternalId());
        shop.setTokenEncrypted(encryptionResult.encryptedData());
        shop.setTokenIv(encryptionResult.iv());
        return save(shop);
    }

    List<Shop> findByUserId(UUID userId);

    Page<Shop> findByUserId(UUID userId, Pageable pageable);

    Optional<Shop> findByIdAndUserId(UUID shopId, UUID userId);

    Optional<Shop> findByExternalId(String externalId);

    @Query("SELECT s FROM Shop s WHERE s.tokenEncrypted IS NOT NULL")
    List<Shop> findAllWithTokens();

    @Query("SELECT s FROM Shop s WHERE s.user.id = :userId AND s.id = :shopId")
    Optional<Shop> findByIdAndUserIdWithUser(@Param("shopId") UUID shopId, @Param("userId") UUID userId);
}
