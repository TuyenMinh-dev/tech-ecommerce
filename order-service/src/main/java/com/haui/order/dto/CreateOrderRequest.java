package com.haui.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String shippingAddress;
    private String notes;
    private List<OrderItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private String productId;
        private String productName;
        private BigDecimal unitPrice; // Optional: nếu không truyền hoặc để xác thực với Catalog
        private Integer quantity;
    }
}
