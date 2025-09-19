package com.openmarket.controller;

import com.openmarket.dto.counterparty.CounterpartyContractResponse;
import com.openmarket.dto.counterparty.CreateCounterpartyContractRequest;
import com.openmarket.dto.counterparty.UpdateCounterpartyContractRequest;
import com.openmarket.security.UserPrincipal;
import com.openmarket.service.CounterpartyContractService;
import com.openmarket.utils.LogWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/shops/{shopId}/counterparties")
public class CounterpartyContractController {

    private final CounterpartyContractService counterpartyContractService;

    @PostMapping
    public ResponseEntity<CounterpartyContractResponse> createCounterpartyContract(@PathVariable UUID shopId,
                                                                                   @Valid @RequestBody CreateCounterpartyContractRequest request,
                                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "createCounterpartyContract",
                () -> counterpartyContractService.createCounterpartyContract(shopId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CounterpartyContractResponse>> getCounterpartyContracts(@PathVariable UUID shopId,
                                                                                       @RequestParam(required = false) String search,
                                                                                       @PageableDefault(size = 20) Pageable pageable,
                                                                                       @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getCounterpartyContracts",
                () -> counterpartyContractService.getCounterpartyContracts(shopId, principal.getUserId(), search, pageable)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<CounterpartyContractResponse>> getCounterpartyContractsList(@PathVariable UUID shopId,
                                                                                           @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getCounterpartyContractsList",
                () -> counterpartyContractService.getCounterpartyContractsList(shopId, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{contractId}")
    public ResponseEntity<CounterpartyContractResponse> getCounterpartyContract(@PathVariable UUID shopId,
                                                                                @PathVariable UUID contractId,
                                                                                @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "getCounterpartyContract",
                () -> counterpartyContractService.getCounterpartyContract(shopId, contractId, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{contractId}")
    public ResponseEntity<CounterpartyContractResponse> updateCounterpartyContract(@PathVariable UUID shopId,
                                                                                   @PathVariable UUID contractId,
                                                                                   @Valid @RequestBody UpdateCounterpartyContractRequest request,
                                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        var response = LogWrapper.logWrap(
                log,
                "updateCounterpartyContract",
                () -> counterpartyContractService.updateCounterpartyContract(shopId, contractId, request, principal.getUserId())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> deleteCounterpartyContract(@PathVariable UUID shopId,
                                                           @PathVariable UUID contractId,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        LogWrapper.logWrap(
                log,
                "deleteCounterpartyContract",
                () -> {
                    counterpartyContractService.deleteCounterpartyContract(shopId, contractId, principal.getUserId());
                    return true;
                }
        );
        return ResponseEntity.noContent().build();
    }
}

