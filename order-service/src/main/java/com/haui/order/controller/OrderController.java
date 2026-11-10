package com.haui.order.controller;

import com.haui.order.domain.Order;
import com.haui.order.dto.CreateOrderRequest;
import com.haui.order.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderApplicationService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName) {
        try {
            if (headerUserId != null && request.getCustomerId() == null) {
                request.setCustomerId(Long.parseLong(headerUserId));
            }
            if (headerUserName != null && request.getCustomerName() == null) {
                request.setCustomerName(headerUserName);
            }
            Order order = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(required = false) Long customerId) {
        if (customerId != null) {
            return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Order> getOrderByCode(@PathVariable String code) {
        return orderService.getOrderByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<?> markPaid(@PathVariable Long id) {
        try {
            Order order = orderService.markAsPaid(id);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Khách hàng yêu cầu hủy";
            Order order = orderService.cancelOrder(id, reason);
            return ResponseEntity.ok(order);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
