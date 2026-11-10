package com.haui.catalog.repository;

import com.haui.catalog.model.Product;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReactiveProductRepository extends ReactiveMongoRepository<Product, String> {

    Mono<Product> findBySku(String sku);

    Flux<Product> findByCategoryIgnoreCase(String category);

    Flux<Product> findByBrandIgnoreCase(String brand);

    Flux<Product> findByStockGreaterThan(Integer stock);
}
