package com.haui.order.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate Root: Order
 * Quản lý vòng đời và duy trì các bất biến nghiệp vụ (Invariants) của đơn hàng linh kiện.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", unique = true, nullable = false, length = 64)
    private String orderCode;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "total_currency"))
    })
    @Builder.Default
    private Money totalAmount = Money.zero();

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void addItem(String productId, String productName, BigDecimal unitPrice, int quantity) {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Không thể thêm mặt hàng vào đơn hàng không ở trạng thái PENDING!");
        }
        OrderItem item = OrderItem.create(productId, productName, unitPrice, quantity);
        item.setOrder(this);
        this.items.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        Money sum = Money.zero();
        for (OrderItem item : items) {
            sum = sum.plus(item.getSubtotal());
        }
        this.totalAmount = sum;
    }

    public void markAsPaid() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng đã bị hủy, không thể thanh toán!");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.PAID) {
            throw new IllegalStateException("Đơn hàng đã thanh toán không thể tự ý hủy trực tiếp!");
        }
        this.status = OrderStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes + " | " : "") + "Lý do hủy: " + reason;
    }
}
