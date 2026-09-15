package com.banking.domain;

import java.util.Objects;

/**
 * Opaque identity for an account. Equality is by value so the same customer
 * account is recognised regardless of which aggregate instance we hold.
 */
public final class AccountId implements Comparable<AccountId> {
    private final String value;

    public AccountId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Account id is required");
        }
        this.value = value.trim();
    }

    public String value() {
        return value;
    }

    @Override
    public int compareTo(AccountId other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccountId accountId)) {
            return false;
        }
        return value.equals(accountId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
