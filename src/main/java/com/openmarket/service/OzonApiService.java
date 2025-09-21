package com.openmarket.service;

import com.openmarket.dto.ozon.*;
import com.openmarket.exception.OzonApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for integrating with Ozon Seller API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OzonApiService {

    private static final String CLIENT_ID_HEADER = "Client-Id";
    private static final String API_KEY_HEADER = "Api-Key";
    private static final String PRODUCT_LIST_URL = "/v3/product/list";
    private static final String PRODUCT_INFO_URL = "/v3/product/info/list";
    private static final String SUPPLY_ORDER_LIST_URL = "/v2/supply-order/list";
    private static final String SUPPLY_ORDER_GET_URL = "/v2/supply-order/get";
    private static final String SUPPLY_ORDER_BUNDLE_URL = "/v1/supply-order/bundle";
    private final WebClient.Builder webClientBuilder;
    @Value("${app.ozon.api.base-url}")
    private String baseUrl;
    @Value("${app.ozon.api.timeout}")
    private int timeout;
    @Value("${app.ozon.api.retry-attempts}")
    private int retryAttempts;

    /**
     * Get product list from Ozon API
     */
    public List<ProductListItem> getProductList(String clientId, String apiKey) {
        WebClient webClient = buildWebClient(clientId, apiKey);
        ProductListRequest request = ProductListRequest.builder().filter(new ProductListFilter("ALL")).limit(1000).build();
        try {
            ProductListResponse response = getProductListResponse(webClient, request);
            if (Objects.isNull(response) || Objects.isNull(response.getResult()) || Objects.isNull(response.getResult().getItems())) {
                return List.of();
            }
            List<ProductListItem> productListItems = new ArrayList<>();
            while (Objects.nonNull(response)
                    && Objects.nonNull(response.getResult())
                    && Objects.nonNull(response.getResult().getItems())) {
                log.info("Retrieved {} products from Ozon for client: {}", response.getResult().getItems().size(), clientId);
                ProductListResult result = response.getResult();
                productListItems.addAll(result.getItems());
                if (result.getTotal() > productListItems.size()) {
                    request.setLastId(result.getLastId());
                    response = getProductListResponse(webClient, request);
                } else {
                    response = null;
                }
            }
            return productListItems;
        } catch (WebClientResponseException.Unauthorized e) {
            throw new OzonApiException(handleUnauthorizedException(clientId), e);
        } catch (Exception e) {
            log.error("Error getting product list from Ozon for client: {}", clientId, e);
            throw new OzonApiException("Failed to retrieve product list", e);
        }
    }

    private ProductListResponse getProductListResponse(WebClient webClient, ProductListRequest request) {
        return webClient.post()
                .uri(PRODUCT_LIST_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProductListResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(getRetrySpec())
                .block();
    }

    /**
     * Get detailed product information
     */
    public List<ProductInfo> getProductInfo(String clientId, String apiKey, List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        WebClient webClient = buildWebClient(clientId, apiKey);
        // Process in batches of 100 (Ozon API limit)
        List<ProductInfo> allProducts = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i += 100) {
            int endIndex = Math.min(i + 100, productIds.size());
            List<Long> batch = productIds.subList(i, endIndex);
            ProductInfoRequest request = ProductInfoRequest.builder().productIds(batch).build();
            try {
                ProductInfoResult response = getProductInfoResult(webClient, request);
                if (Objects.nonNull(response) && Objects.nonNull(response.getItems())) {
                    allProducts.addAll(response.getItems());
                }
            } catch (WebClientResponseException.Unauthorized e) {
                handleUnauthorizedException(clientId);
                throw new OzonApiException("Invalid API credentials", e);
            } catch (Exception e) {
                log.error("Error getting product info from Ozon for client: {}, batch: {}", clientId, batch, e);
                // Continue with other batches on error
            }
        }
        log.info("Retrieved detailed info for {} products from Ozon for client: {}", allProducts.size(), clientId);
        return allProducts;
    }

    private ProductInfoResult getProductInfoResult(WebClient webClient, ProductInfoRequest request) {
        return webClient.post()
                .uri(PRODUCT_INFO_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProductInfoResult.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(getRetrySpec())
                .block();
    }

    private WebClient buildWebClient(String clientId, String apiKey) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(CLIENT_ID_HEADER, clientId)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
    }

    private RetryBackoffSpec getRetrySpec() {
        return Retry.fixedDelay(retryAttempts, Duration.ofSeconds(2))
                .filter(throwable -> !(throwable instanceof WebClientResponseException.Unauthorized));
    }

    /**
     * Get supply order list from Ozon API
     */
    public List<String> getSupplyOrderList(String clientId, String apiKey) {
        WebClient webClient = buildWebClient(clientId, apiKey);
        List<String> allSupplyOrderIds = new ArrayList<>();
        Long lastSupplyOrderId;
        try {
            SupplyOrderListResult response = getSupplyOrderListResponse(null, webClient);
            while (Objects.nonNull(response) && Objects.nonNull(response.getSupplyOrderId())) {
                List<String> currentBatch = response.getSupplyOrderId();
                allSupplyOrderIds.addAll(currentBatch);
                lastSupplyOrderId = response.getLastSupplyOrderId();
                log.info("Retrieved {} supply orders from Ozon for client: {}", currentBatch.size(), clientId);
                if (currentBatch.isEmpty() || Objects.isNull(lastSupplyOrderId)) {
                    break;
                }
                response = (lastSupplyOrderId > 0) ? getSupplyOrderListResponse(lastSupplyOrderId, webClient) : null;
            }
            log.info("Total supply orders retrieved: {} for client: {}", allSupplyOrderIds.size(), clientId);
            return allSupplyOrderIds;
        } catch (WebClientResponseException.Unauthorized e) {
            throw new OzonApiException(handleUnauthorizedException(clientId), e);
        } catch (Exception e) {
            log.error("Error getting supply order list from Ozon for client: {}", clientId, e);
            throw new OzonApiException("Failed to retrieve supply order list", e);
        }
    }

    private SupplyOrderListResult getSupplyOrderListResponse(Long fromSupplyOrderId, WebClient webClient) {
        SupplyOrderFilter filter = SupplyOrderFilter.builder().states(new String[]{"ORDER_STATE_COMPLETED"}).build();
        SupplyOrderPaging paging = SupplyOrderPaging.builder().limit(50).fromSupplyOrderId(fromSupplyOrderId).build();
        SupplyOrderListRequest request = SupplyOrderListRequest.builder().filter(filter).paging(paging).build();
        return webClient.post()
                .uri(SUPPLY_ORDER_LIST_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SupplyOrderListResult.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(getRetrySpec())
                .block();
    }

    /**
     * Get supply order details
     */
    public List<SupplyOrderInfo> getSupplyOrderDetails(String clientId, String apiKey, List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        WebClient webClient = buildWebClient(clientId, apiKey);
        try {
            List<SupplyOrderInfo> allOrders = new ArrayList<>();
            for (int i = 0; i < orderIds.size(); i += 50) {
                int endIndex = Math.min(i + 50, orderIds.size());
                List<String> batch = orderIds.subList(i, endIndex);
                SupplyOrderGetResponse response = getSupplyOrderGetResponse(batch, webClient);
                if (Objects.nonNull(response) && Objects.nonNull(response.getOrders())) {
                    List<SupplyOrderInfo> supplyOrderInfos = response.getOrders()
                            .stream()
                            .map(r -> SupplyOrderInfo.builder()
                                    .supplyOrderNumber(r.getSupplyOrderNumber())
                                    .creationDate(r.getCreationDate())
                                    .bundleIdsBySupplyOrderNumber(r.getSupplies().stream().collect(Collectors.groupingBy(sr -> sr.getSupplyId().toString(), Collectors.mapping(SupplyResponse::getBundleId, Collectors.toList()))))
                                    .supplyOrderId(r.getSupplyOrderId())
                                    .build()
                            )
                            .toList();
                    allOrders.addAll(supplyOrderInfos);
                }
            }
            log.info("Retrieved details for {} supply orders from Ozon for client: {}", allOrders.size(), clientId);
            return allOrders;
        } catch (WebClientResponseException.Unauthorized e) {
            throw new OzonApiException(handleUnauthorizedException(clientId), e);
        } catch (Exception e) {
            log.error("Error getting supply order details from Ozon for client: {}", clientId, e);
            throw new OzonApiException("Failed to retrieve supply order details", e);
        }
    }

    private SupplyOrderGetResponse getSupplyOrderGetResponse(List<String> batch, WebClient webClient) {
        SupplyOrderGetRequest request = SupplyOrderGetRequest.builder().orderIds(batch).build();
        return webClient.post()
                .uri(SUPPLY_ORDER_GET_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SupplyOrderGetResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(getRetrySpec())
                .block();
    }

    /**
     * Get supply order bundle items
     */
    public List<SupplyOrderBundleItem> getSupplyOrderBundleItems(String clientId, String apiKey, List<String> bundleIds) {
        if (bundleIds.isEmpty()) {
            return List.of();
        }
        WebClient webClient = buildWebClient(clientId, apiKey);
        try {
            List<SupplyOrderBundleItem> allItems = new ArrayList<>();
            String lastId;
            SupplyOrderBundleResult response = getSupplyOrderBundleResponse(bundleIds, null, webClient);
            while (Objects.nonNull(response) && Objects.nonNull(response.getItems())) {
                List<SupplyOrderBundleItem> currentBatch = response.getItems();
                allItems.addAll(currentBatch);
                lastId = response.getLastId();
                if (currentBatch.isEmpty() || Objects.isNull(lastId)) {
                    break;
                }
                response = lastId.equals("0") ? null : getSupplyOrderBundleResponse(bundleIds, lastId, webClient);
            }
            log.info("Retrieved {} bundle items from Ozon for client: {}", allItems.size(), clientId);
            return allItems;
        } catch (WebClientResponseException.Unauthorized e) {
            throw new OzonApiException(handleUnauthorizedException(clientId), e);
        } catch (Exception e) {
            log.error("Error getting supply order bundle items from Ozon for client: {}", clientId, e);
            throw new OzonApiException("Failed to retrieve supply order bundle items", e);
        }
    }

    private SupplyOrderBundleResult getSupplyOrderBundleResponse(List<String> bundleIds, String lastId, WebClient webClient) {
        SupplyOrderBundleRequest request =
                SupplyOrderBundleRequest.builder().bundleIds(bundleIds).lastId(lastId).limit(50).build();
        return webClient.post()
                .uri(SUPPLY_ORDER_BUNDLE_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SupplyOrderBundleResult.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(getRetrySpec())
                .block();
    }

    private static String handleUnauthorizedException(String clientId) {
        log.error("Unauthorized access to Ozon API for client: {}", clientId);
        return "Invalid API credentials";
    }
}
