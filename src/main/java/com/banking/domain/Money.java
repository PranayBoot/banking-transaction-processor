package com.banking.domain;

import com.banking.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A non-negative amount of a single implied currency, stored at cent precision.
 * Extra fractional digits are rejected rather than rounded, so callers never
 * lose money to silent rounding.
 */
public final class Money implements Comparable<Money> {
    private static final int SCALE = 2;

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money of(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new InvalidAmountException("Amount is required");
        }
        try {
            return of(new BigDecimal(amount.trim()));
        } catch (NumberFormatException ex) {
            throw new InvalidAmountException("Amount is not a number: " + amount);
        }
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("Amount is required");
        }
        if (amount.scale() > SCALE) {
            throw new InvalidAmountException("Amount cannot have more than " + SCALE + " decimal places: " + amount);
        }
        if (amount.signum() < 0) {
            throw new InvalidAmountException("Amount cannot be negative: " + amount);
        }
        return new Money(amount.setScale(SCALE, RoundingMode.UNNECESSARY));
    }

    public static Money zero() {
        return of(BigDecimal.ZERO);
    }

    public Money plus(Money other) {
        return new Money(amount.add(require(other).amount));
    }

    public Money minus(Money other) {
        BigDecimal result = amount.subtract(require(other).amount);
        if (result.signum() < 0) {
            throw new InvalidAmountException("Subtraction would produce a negative amount");
        }
        return new Money(result);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(require(other)) < 0;
    }

    public BigDecimal asBigDecimal() {
        return amount;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(require(other).amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }

    private static Money require(Money other) {
        if (other == null) {
            throw new InvalidAmountException("Amount is required");
        }
        return other;
    }
}
