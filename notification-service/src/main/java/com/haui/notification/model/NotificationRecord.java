package com.haui.notification.model;

import java.math.BigDecimal;
import java.time.Instant;

public class NotificationRecord {
    public String id;
    public String orderCode;
    public String customerName;
    public String customerEmail;
    public BigDecimal totalAmount;
    public String status;
    public Instant receivedAt;

    public NotificationRecord() {}

    public NotificationRecord(String id, String orderCode, String customerName, String customerEmail, BigDecimal totalAmount, String status, Instant receivedAt) {
        this.id = id;
        this.orderCode = orderCode;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.receivedAt = receivedAt;
    }
}
