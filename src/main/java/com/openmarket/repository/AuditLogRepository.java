package com.openmarket.repository;

import com.openmarket.entity.AuditLog;
import com.openmarket.entity.Shop;
import com.openmarket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    default AuditLog saveShopAuditLog(UUID userId, String action, Shop shop) {
        AuditLog auditLog = AuditLog.create(userId, "CREATE_SHOP", "SHOP", shop.getId());
        auditLog.setPayload(Map.of(
                "name", shop.getName(),
                "externalId", Objects.nonNull(shop.getExternalId()) ? shop.getExternalId() : "",
                "hasToken", Objects.nonNull(shop.getTokenEncrypted())
        ));
        return save(auditLog);
    }

    default AuditLog saveShopAuditLog(UUID userId, Map<String, Object> changes, Shop shop) {
        AuditLog auditLog = AuditLog.create(userId, "UPDATE_SHOP", "SHOP", shop.getId());
        auditLog.setPayload(changes);
        return save(auditLog);
    }

    default AuditLog saveNomenclatureAuditLog(UUID userId, String action, Map<String, Object> changes, Shop shop) {
        AuditLog auditLog = AuditLog.create(userId, "UPDATE_NOMENCLATURE_MANUAL", "SHOP", shop.getId());
        auditLog.setPayload(changes);
        return save(auditLog);
    }

    default AuditLog saveUserAuditLog(UUID userId, String action, Map<String, Object> changes) {
        AuditLog auditLog = AuditLog.create(userId, action, "USER", userId);
        auditLog.setPayload(changes);
        return save(auditLog);
    }

    default AuditLog saveUserAuditLog(UUID actorId, String action, Map<String, Object> changes, User user) {
        AuditLog auditLog = AuditLog.create(actorId, action, "USER", user.getId());
        auditLog.setPayload(changes);
        return save(auditLog);
    }

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :fromDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findRecentLogs(@Param("fromDate") OffsetDateTime fromDate, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.action = :action AND a.createdAt >= :fromDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findByActionAndDateRange(@Param("action") String action, 
                                          @Param("fromDate") OffsetDateTime fromDate, 
                                          Pageable pageable);
}
