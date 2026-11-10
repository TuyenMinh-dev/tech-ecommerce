package com.haui.catalog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    private String sku;

    private String name;

    private String category; // CPU, GPU, RAM, MAINBOARD, STORAGE, PERIPHERAL

    private String brand;

    private BigDecimal price;

    private Integer stock;

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, OUT_OF_STOCK, DISCONTINUED

    /**
     * Bảng thông số kỹ thuật động (Dynamic Hardware Specifications)
     * Ví dụ CPU: {"socket": "AM5", "cores": 6, "threads": 12, "baseClock": "4.7GHz"}
     * Ví dụ GPU: {"vram": "12GB GDDR6X", "tdp": "220W", "interface": "PCIe 4.0"}
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
