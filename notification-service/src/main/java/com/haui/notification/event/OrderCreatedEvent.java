package com.haui.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedEvent {

    public String orderCode;
    public Long orderId;
    public Long customerId;
    public String customerName;
    public String customerEmail;
    public String shippingAddress;
    public BigDecimal totalAmount;
    public String currency;
    public List<OrderItemSummary> items;
    public Instant createdAt;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemSummary {
        public String productId;
        public String productName;
        public BigDecimal unitPrice;
        public Integer quantity;
        public BigDecimal subtotal;
    }
}
