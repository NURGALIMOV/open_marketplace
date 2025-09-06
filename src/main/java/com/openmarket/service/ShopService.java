package com.openmarket.service;

import com.openmarket.dto.auth.EncryptionResult;
import com.openmarket.dto.shop.CreateShopRequest;
import com.openmarket.dto.shop.ShopResponse;
import com.openmarket.dto.shop.UpdateShopRequest;
import com.openmarket.entity.Shop;
import com.openmarket.entity.User;
import com.openmarket.exception.AppBusinessException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.NomenclatureRepository;
import com.openmarket.repository.ShopRepository;
import com.openmarket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final AuditLogRepository auditLogRepository;
    private final EncryptionService encryptionService;

    /**
     * Create a new shop
     */
    @Transactional
    public ShopResponse createShop(CreateShopRequest request, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppNotFoundException("User not found"));
        EncryptionResult encryptionResult = encryptionService.encrypt(request.getToken());
        Shop shop = shopRepository.saveShop(user, request, encryptionResult);
        auditLogRepository.saveShopAuditLog(userId, "CREATE_SHOP", shop);
        log.info("Shop created: {} by user: {}", shop.getName(), user.getEmail());
        return ShopResponse.from(shop, 0L);
    }

    /**
     * Get shops for user
     */
    public List<ShopResponse> getUserShops(UUID userId) {
        return shopRepository.findByUserId(userId)
                .stream()
                .map(shop -> ShopResponse.from(shop, nomenclatureRepository.countByShopId(shop.getId())))
                .toList();
    }

    /**
     * Get shops for user with pagination
     */
    public Page<ShopResponse> getUserShops(UUID userId, Pageable pageable) {
        return shopRepository.findByUserId(userId, pageable)
                .map(shop -> ShopResponse.from(shop, nomenclatureRepository.countByShopId(shop.getId())));
    }

    /**
     * Get shop by ID (user must own the shop)
     */
    public Optional<ShopResponse> getShop(UUID shopId, UUID userId) {
        return shopRepository.findByIdAndUserId(shopId, userId)
                .map(shop -> ShopResponse.from(shop, nomenclatureRepository.countByShopId(shop.getId())));
    }

    /**
     * Get shop entity by ID (user must own the shop)
     */
    public Optional<Shop> getShopEntity(UUID shopId, UUID userId) {
        return Objects.isNull(userId) ? shopRepository.findById(shopId) : shopRepository.findByIdAndUserId(shopId, userId);
    }

    /**
     * Update shop
     */
    @Transactional
    public ShopResponse updateShop(UUID shopId, UpdateShopRequest request, UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException("Shop not found"));
        Map<String, Object> changes = getChanges(request, shop);
        if (!changes.isEmpty()) {
            shop = shopRepository.save(shop);
            auditLogRepository.saveShopAuditLog(userId, changes, shop);
            log.info("Shop updated: {} by user: {}", shop.getName(), userId);
        }
        return ShopResponse.from(shop, nomenclatureRepository.countByShopId(shop.getId()));
    }

    private Map<String, Object> getChanges(UpdateShopRequest request, Shop shop) {
        Map<String, Object> changes = new HashMap<>();
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(shop.getName())) {
            changes.put("name", Map.of("old", shop.getName(), "new", request.getName()));
            shop.setName(request.getName());
        }
        if (Objects.nonNull(request.getExternalId()) && !request.getExternalId().equals(shop.getExternalId())) {
            changes.put("externalId", Map.of("old", shop.getExternalId(), "new", request.getExternalId()));
            shop.setExternalId(request.getExternalId());
        }
        if (StringUtils.hasText(request.getToken())) {
            EncryptionResult encryptionResult = encryptionService.encrypt(request.getToken());
            shop.setTokenEncrypted(encryptionResult.encryptedData());
            shop.setTokenIv(encryptionResult.iv());
            changes.put("token", "updated");
        }
        return changes;
    }

    /**
     * Delete shop
     */
    @Transactional
    public void deleteShop(UUID shopId, UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId)
                .orElseThrow(() -> new AppNotFoundException("Shop by id %s not found".formatted(shopId)));
        auditLogRepository.saveShopAuditLog(userId, "DELETE_SHOP", shop);
        shopRepository.delete(shop);
        log.info("Shop deleted: {} by user: {}", shop.getName(), userId);
    }

    /**
     * Get decrypted token for shop (internal use)
     */
    public String getDecryptedToken(Shop shop) {
        if (Objects.isNull(shop.getTokenEncrypted()) || Objects.isNull(shop.getTokenIv())) {
            return null;
        }
        try {
            return encryptionService.decrypt(shop.getTokenEncrypted(), shop.getTokenIv());
        } catch (Exception e) {
            log.error("Failed to decrypt token for shop: {}", shop.getId(), e);
            throw new AppBusinessException("Failed to decrypt shop token");
        }
    }

    /**
     * Get all shops with tokens (for system operations)
     */
    public List<Shop> getAllShopsWithTokens() {
        return shopRepository.findAllWithTokens();
    }
}
