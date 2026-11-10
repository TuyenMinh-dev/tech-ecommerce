package com.haui.order.domain;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object: Money (Đại lượng tiền tệ bất biến)
 * Tránh triệt để lỗi làm tròn số thực của kiểu double/float trong tính toán thương mại.
 */
@Embeddable
@Getter
@ToString
@EqualsAndHashCode
public class Money implements Serializable {

    private final BigDecimal amount;
    private final String currency;

    protected Money() {
        this.amount = BigDecimal.ZERO;
        this.currency = "VND";
    }

    public Money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Số tiền không được phép âm!");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = (currency != null && !currency.isBlank()) ? currency : "VND";
    }

    public static Money ofVnd(BigDecimal amount) {
        return new Money(amount, "VND");
    }

    public static Money ofVnd(long amount) {
        return new Money(BigDecimal.valueOf(amount), "VND");
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, "VND");
    }

    public Money plus(Money other) {
        if (!this.currency.equalsIgnoreCase(other.currency)) {
            throw new IllegalArgumentException("Không thể cộng tiền khác loại tiền tệ (" + this.currency + " vs " + other.currency + ")");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Hệ số nhân không được âm!");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
}
