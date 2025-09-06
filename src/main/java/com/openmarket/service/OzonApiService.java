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

    private static String handleUnauthorizedException(String clientId) {
        log.error("Unauthorized access to Ozon API for client: {}", clientId);
        return "Invalid API credentials";
    }
}
