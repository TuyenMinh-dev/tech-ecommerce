package com.haui.order.service;

import com.haui.order.client.CatalogClient;
import com.haui.order.domain.Order;
import com.haui.order.domain.OrderStatus;
import com.haui.order.dto.CreateOrderRequest;
import com.haui.order.event.OrderCreatedEvent;
import com.haui.order.event.OrderEventPublisher;
import com.haui.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public Order createOrder(CreateOrderRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất 1 linh kiện!");
        }

        String orderCode = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Order order = Order.builder()
                .orderCode(orderCode)
                .customerId(req.getCustomerId() != null ? req.getCustomerId() : 1L)
                .customerName(req.getCustomerName() != null ? req.getCustomerName() : "Khách hàng vãng lai")
                .customerEmail(req.getCustomerEmail() != null ? req.getCustomerEmail() : "customer@example.com")
                .shippingAddress(req.getShippingAddress() != null ? req.getShippingAddress() : "Hà Nội, Việt Nam")
                .notes(req.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        for (CreateOrderRequest.OrderItemRequest itemReq : req.getItems()) {
            BigDecimal price = itemReq.getUnitPrice();
            String productName = itemReq.getProductName();

            // Gọi qua Anti-Corruption Layer (CatalogClient) để chốt giá chính thức (Price Snapshot)
            Optional<CatalogClient.CatalogProductDto> productOpt = catalogClient.getProductById(itemReq.getProductId());
            if (productOpt.isPresent()) {
                CatalogClient.CatalogProductDto catalogProduct = productOpt.get();
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                    price = catalogProduct.getPrice();
                }
                if (productName == null || productName.isBlank()) {
                    productName = catalogProduct.getName();
                }
            } else {
                if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Không xác định được đơn giá cho linh kiện ID: " + itemReq.getProductId());
                }
                if (productName == null || productName.isBlank()) {
                    productName = "Linh kiện công nghệ #" + itemReq.getProductId();
                }
            }

            int qty = (itemReq.getQuantity() != null && itemReq.getQuantity() > 0) ? itemReq.getQuantity() : 1;
            order.addItem(itemReq.getProductId(), productName, price, qty);
        }

        Order saved = orderRepository.save(order);
        log.info("Saved order {} with total amount: {} VND", saved.getOrderCode(), saved.getTotalAmount().getAmount());

        // Phát tán Domain Event lên RabbitMQ
        List<OrderCreatedEvent.OrderItemSummary> itemSummaries = saved.getItems().stream()
                .map(i -> OrderCreatedEvent.OrderItemSummary.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice().getAmount())
                        .quantity(i.getQuantity())
                        .subtotal(i.getSubtotal().getAmount())
                        .build())
                .collect(Collectors.toList());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderCode(saved.getOrderCode())
                .orderId(saved.getId())
                .customerId(saved.getCustomerId())
                .customerName(saved.getCustomerName())
                .customerEmail(saved.getCustomerEmail())
                .shippingAddress(saved.getShippingAddress())
                .totalAmount(saved.getTotalAmount().getAmount())
                .currency(saved.getTotalAmount().getCurrency())
                .items(itemSummaries)
                .createdAt(Instant.now())
                .build();

        try {
            eventPublisher.publishOrderCreated(event);
        } catch (Exception ex) {
            log.warn("Không thể gửi event lên RabbitMQ (Broker có thể chưa chạy): {}", ex.getMessage());
        }

        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByCode(String code) {
        return orderRepository.findByOrderCode(code);
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public Order markAsPaid(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + id));
        order.markAsPaid();
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id, String reason) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + id));
        order.cancel(reason);
        return orderRepository.save(order);
    }
}
