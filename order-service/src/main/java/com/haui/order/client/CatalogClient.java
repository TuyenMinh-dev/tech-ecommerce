package com.haui.order.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Anti-Corruption Layer (ACL)
 * Giao tiếp với Catalog Service để xác thực tính hợp lệ của linh kiện và lấy snapshot giá niêm yết.
 */
@Slf4j
@Component
public class CatalogClient {

    private final WebClient webClient;

    public CatalogClient(
            WebClient.Builder builder,
            @Value("${catalog.service.url}") String catalogUrl) {
        this.webClient = builder.baseUrl(catalogUrl).build();
    }

    public Optional<CatalogProductDto> getProductById(String productId) {
        try {
            CatalogProductDto product = webClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(CatalogProductDto.class)
                    .block();
            return Optional.ofNullable(product);
        } catch (Exception ex) {
            log.warn("Không thể kết nối tới Catalog Service để lấy thông tin sản phẩm {}: {}", productId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Data
    public static class CatalogProductDto {
        private String id;
        private String sku;
        private String name;
        private String category;
        private String brand;
        private BigDecimal price;
        private Integer stock;
        private String status;
    }
}
