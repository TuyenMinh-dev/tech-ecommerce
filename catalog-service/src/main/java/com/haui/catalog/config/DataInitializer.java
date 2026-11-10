package com.haui.catalog.config;

import com.haui.catalog.model.Product;
import com.haui.catalog.repository.ReactiveProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReactiveProductRepository productRepository;

    @Override
    public void run(String... args) {
        productRepository.count()
                .filter(count -> count == 0)
                .flatMapMany(count -> {
                    log.info("Kho dữ liệu Catalog trống, đang tự động nạp linh kiện mẫu (Data Seeding)...");
                    
                    Product p1 = Product.builder()
                            .sku("CPU-AMD-R5-7600X")
                            .name("CPU AMD Ryzen 5 7600X")
                            .category("CPU")
                            .brand("AMD")
                            .price(new BigDecimal("5790000"))
                            .stock(45)
                            .status("ACTIVE")
                            .attributes(Map.of(
                                    "socket", "AM5",
                                    "cores", 6,
                                    "threads", 12,
                                    "baseClock", "4.7 GHz",
                                    "boostClock", "5.3 GHz",
                                    "tdp", "105W",
                                    "cache", "38MB"
                            ))
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    Product p2 = Product.builder()
                            .sku("VGA-ASUS-RTX4070S-12G")
                            .name("Card màn hình ASUS Dual GeForce RTX 4070 SUPER 12GB")
                            .category("GPU")
                            .brand("ASUS")
                            .price(new BigDecimal("17490000"))
                            .stock(20)
                            .status("ACTIVE")
                            .attributes(Map.of(
                                    "vram", "12GB GDDR6X",
                                    "busWidth", "192-bit",
                                    "cudaCores", 7168,
                                    "recommendedPsu", "750W",
                                    "interface", "PCIe 4.0"
                            ))
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    Product p3 = Product.builder()
                            .sku("RAM-CORSAIR-VEN-32G-DDR5")
                            .name("RAM Corsair Vengeance 32GB (2x16GB) DDR5 6000MHz RGB")
                            .category("RAM")
                            .brand("Corsair")
                            .price(new BigDecimal("3150000"))
                            .stock(60)
                            .status("ACTIVE")
                            .attributes(Map.of(
                                    "type", "DDR5",
                                    "capacity", "32GB (2x16GB)",
                                    "bus", "6000MHz",
                                    "casLatency", "CL36",
                                    "rgb", true
                            ))
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    Product p4 = Product.builder()
                            .sku("SSD-SAMSUNG-990PRO-1TB")
                            .name("Ổ cứng SSD Samsung 990 Pro 1TB M.2 NVMe PCIe Gen 4.0")
                            .category("STORAGE")
                            .brand("Samsung")
                            .price(new BigDecimal("2990000"))
                            .stock(80)
                            .status("ACTIVE")
                            .attributes(Map.of(
                                    "formFactor", "M.2 2280",
                                    "interface", "PCIe Gen 4.0 x4, NVMe 2.0",
                                    "readSpeed", "7450 MB/s",
                                    "writeSpeed", "6900 MB/s"
                            ))
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return productRepository.saveAll(List.of(p1, p2, p3, p4));
                })
                .doOnComplete(() -> log.info("Nạp dữ liệu Catalog mẫu thành công!"))
                .subscribe();
    }
}
