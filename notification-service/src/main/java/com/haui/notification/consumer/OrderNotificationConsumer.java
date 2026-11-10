package com.haui.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haui.notification.event.OrderCreatedEvent;
import com.haui.notification.model.NotificationRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@ApplicationScoped
public class OrderNotificationConsumer {

    private static final Logger LOG = Logger.getLogger(OrderNotificationConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentLinkedDeque<NotificationRecord> recentNotifications = new ConcurrentLinkedDeque<>();

    @Incoming("order-in")
    public void consume(byte[] rawMessage) {
        try {
            String payload = new String(rawMessage, StandardCharsets.UTF_8);
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            LOG.infof("================================================================================");
            LOG.infof("🔔 [QUARKUS NOTIFICATION SERVICE] NHẬN SỰ KIỆN TẠO ĐƠN HÀNG MỚI TỪ RABBITMQ!");
            LOG.infof("📦 Mã đơn hàng: %s", event.orderCode);
            LOG.infof("👤 Khách hàng: %s (Email: %s)", event.customerName, event.customerEmail);
            LOG.infof("📍 Địa chỉ nhận hàng: %s", event.shippingAddress);
            LOG.infof("💰 Tổng tiền thanh toán: %s %s", event.totalAmount, event.currency);
            if (event.items != null) {
                LOG.infof("📋 Danh sách linh kiện công nghệ đã mua (%d sản phẩm):", event.items.size());
                for (OrderCreatedEvent.OrderItemSummary item : event.items) {
                    LOG.infof("   - [%s] %s | Đơn giá: %s VND x %d = %s VND",
                            item.productId, item.productName, item.unitPrice, item.quantity, item.subtotal);
                }
            }
            LOG.infof("📨 Đang tiến hành giả lập gửi Email xác nhận đơn hàng thành công tới: %s", event.customerEmail);
            LOG.infof("✅ [QUARKUS] Xử lý sự kiện hoàn tất siêu tốc (Reactive Non-blocking, RSS RAM < 35MB)!");
            LOG.infof("================================================================================");

            NotificationRecord record = new NotificationRecord(
                    UUID.randomUUID().toString(),
                    event.orderCode,
                    event.customerName,
                    event.customerEmail,
                    event.totalAmount,
                    "SENT",
                    Instant.now()
            );

            recentNotifications.addFirst(record);
            if (recentNotifications.size() > 50) {
                recentNotifications.removeLast();
            }

        } catch (Exception ex) {
            LOG.errorf("Lỗi khi xử lý message từ RabbitMQ: %s", ex.getMessage());
        }
    }

    public List<NotificationRecord> getRecentNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(recentNotifications));
    }
}
