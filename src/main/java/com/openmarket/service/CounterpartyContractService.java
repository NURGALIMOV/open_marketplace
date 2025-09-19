package com.openmarket.service;

import com.openmarket.dto.counterparty.CounterpartyContractResponse;
import com.openmarket.dto.counterparty.CreateCounterpartyContractRequest;
import com.openmarket.dto.counterparty.UpdateCounterpartyContractRequest;
import com.openmarket.entity.CounterpartyContract;
import com.openmarket.entity.Shop;
import com.openmarket.exception.AppAlreadyExistException;
import com.openmarket.exception.AppNotFoundException;
import com.openmarket.repository.AuditLogRepository;
import com.openmarket.repository.CounterpartyContractRepository;
import com.openmarket.repository.ShopRepository;
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
public class CounterpartyContractService {

    private static final String SHOP_NOT_FOUND = "Shop not found";
    private static final String COUNTERPARTY_CONTRACT_NOT_FOUND = "Counterparty contract not found";
    private final CounterpartyContractRepository counterpartyContractRepository;
    private final ShopRepository shopRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Create a new counterparty contract
     */
    @Transactional
    public CounterpartyContractResponse createCounterpartyContract(UUID shopId, CreateCounterpartyContractRequest request, UUID userId) {
        Shop shop = shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        if (counterpartyContractRepository.existsByShopIdAndCounterpartyAndContract(shopId, request.getCounterparty(), request.getContract())) {
            throw new AppAlreadyExistException("Counterparty contract already exists");
        }
        CounterpartyContract entity = counterpartyContractRepository.saveCounterpartyContract(request, shop);
        auditLogRepository.saveCounterpartyContractAuditLog(entity, userId, "CREATE_COUNTERPARTY_CONTRACT");
        log.info("Counterparty contract created: {} - {} for shop: {}", entity.getCounterparty(), entity.getContract(), shop.getName());
        return CounterpartyContractResponse.from(entity);
    }

    /**
     * Get counterparty contracts for shop
     */
    public Page<CounterpartyContractResponse> getCounterpartyContracts(UUID shopId, UUID userId, String search, Pageable pageable) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        Page<CounterpartyContract> entities = StringUtils.hasText(search) ?
                counterpartyContractRepository.findByShopIdAndSearch(shopId, search, pageable) :
                counterpartyContractRepository.findByShopId(shopId, pageable);
        return entities.map(CounterpartyContractResponse::from);
    }

    /**
     * Get all counterparty contracts for shop (no pagination)
     */
    public List<CounterpartyContractResponse> getCounterpartyContractsList(UUID shopId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        return counterpartyContractRepository.findByShopId(shopId).stream().map(CounterpartyContractResponse::from).toList();
    }

    /**
     * Get counterparty contract by ID
     */
    public CounterpartyContractResponse getCounterpartyContract(UUID shopId, UUID contractId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        CounterpartyContract entity =
                counterpartyContractRepository.findByIdAndShopId(contractId, shopId).orElseThrow(() -> new AppNotFoundException(COUNTERPARTY_CONTRACT_NOT_FOUND));
        return CounterpartyContractResponse.from(entity);
    }

    /**
     * Update counterparty contract
     */
    @Transactional
    public CounterpartyContractResponse updateCounterpartyContract(UUID shopId,
                                                                   UUID contractId,
                                                                   UpdateCounterpartyContractRequest request,
                                                                   UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        CounterpartyContract entity =
                counterpartyContractRepository.findByIdAndShopId(contractId, shopId).orElseThrow(() -> new AppNotFoundException(COUNTERPARTY_CONTRACT_NOT_FOUND));
        Map<String, Object> changes = new HashMap<>();
        if (StringUtils.hasText(request.getCounterparty()) && !request.getCounterparty().equals(entity.getCounterparty())) {
            changes.put("counterparty", Map.of("old", entity.getCounterparty(), "new", request.getCounterparty()));
            entity.setCounterparty(request.getCounterparty());
        }
        if (StringUtils.hasText(request.getContract()) && !request.getContract().equals(entity.getContract())) {
            changes.put("contract", Map.of("old", entity.getContract(), "new", request.getContract()));
            entity.setContract(request.getContract());
        }
        if (Objects.nonNull(request.getContractDate()) && !request.getContractDate().equals(entity.getContractDate())) {
            changes.put("contractDate", Map.of("old", entity.getContractDate(), "new", request.getContractDate()));
            entity.setContractDate(request.getContractDate());
        }
        if (!changes.isEmpty()) {
            entity = counterpartyContractRepository.save(entity);
            auditLogRepository.saveCounterpartyContractAuditLog(userId, entity, changes);
            log.info("Counterparty contract updated: {} - {}", entity.getCounterparty(), entity.getContract());
        }
        return CounterpartyContractResponse.from(entity);
    }

    /**
     * Delete counterparty contract
     */
    @Transactional
    public void deleteCounterpartyContract(UUID shopId, UUID contractId, UUID userId) {
        shopRepository.findByIdAndUserId(shopId, userId).orElseThrow(() -> new AppNotFoundException(SHOP_NOT_FOUND));
        CounterpartyContract entity =
                counterpartyContractRepository.findByIdAndShopId(contractId, shopId).orElseThrow(() -> new AppNotFoundException(COUNTERPARTY_CONTRACT_NOT_FOUND));
        auditLogRepository.saveCounterpartyContractAuditLog(entity, userId, "DELETE_COUNTERPARTY_CONTRACT");
        counterpartyContractRepository.delete(entity);
        log.info("Counterparty contract deleted: {} - {}", entity.getCounterparty(), entity.getContract());
    }
}

