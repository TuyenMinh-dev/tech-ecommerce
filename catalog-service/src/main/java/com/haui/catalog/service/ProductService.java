package com.haui.catalog.service;

import com.haui.catalog.model.Product;
import com.haui.catalog.repository.ReactiveProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ReactiveProductRepository productRepository;

    // Reactive Sink để phát sóng (broadcast) sự kiện cập nhật linh kiện thời gian thực (SSE)
    private final Sinks.Many<Product> productSink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<Product> getAllProducts(String category) {
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryIgnoreCase(category);
        }
        return productRepository.findAll();
    }

    public Mono<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Mono<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    public Mono<Product> createProduct(Product product) {
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        if (product.getStatus() == null) {
            product.setStatus("ACTIVE");
        }
        return productRepository.save(product)
                .doOnSuccess(saved -> {
                    log.info("Created new product: {} (ID: {})", saved.getName(), saved.getId());
                    productSink.tryEmitNext(saved);
                });
    }

    public Mono<Product> updateProduct(String id, Product updateReq) {
        return productRepository.findById(id)
                .flatMap(existing -> {
                    if (updateReq.getName() != null) existing.setName(updateReq.getName());
                    if (updateReq.getCategory() != null) existing.setCategory(updateReq.getCategory());
                    if (updateReq.getBrand() != null) existing.setBrand(updateReq.getBrand());
                    if (updateReq.getPrice() != null) existing.setPrice(updateReq.getPrice());
                    if (updateReq.getStock() != null) existing.setStock(updateReq.getStock());
                    if (updateReq.getStatus() != null) existing.setStatus(updateReq.getStatus());
                    if (updateReq.getAttributes() != null && !updateReq.getAttributes().isEmpty()) {
                        existing.getAttributes().putAll(updateReq.getAttributes());
                    }
                    existing.setUpdatedAt(Instant.now());
                    return productRepository.save(existing);
                })
                .doOnSuccess(updated -> {
                    if (updated != null) {
                        log.info("Updated product ID: {}", updated.getId());
                        productSink.tryEmitNext(updated);
                    }
                });
    }

    public Mono<Void> deleteProduct(String id) {
        return productRepository.deleteById(id);
    }

    /**
     * Luồng sự kiện Server-Sent Events (SSE) theo thời gian thực
     */
    public Flux<Product> getProductStream() {
        return productSink.asFlux();
    }
}
