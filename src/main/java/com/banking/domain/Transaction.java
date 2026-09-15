package com.banking.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable ledger line. Transfers appear on both accounts: an outgoing
 * debit on the source and an incoming credit on the destination.
 */
public final class Transaction {
    private final String id;
    private final TransactionType type;
    private final Money amount;
    private final Money balanceAfter;
    private final Instant occurredAt;
    private final AccountId counterpartyAccountId;

    public Transaction(
            String id,
            TransactionType type,
            Money amount,
            Money balanceAfter,
            Instant occurredAt,
            AccountId counterpartyAccountId
    ) {
        this.id = requireText(id, "Transaction id is required");
        this.type = Objects.requireNonNull(type, "Transaction type is required");
        this.amount = Objects.requireNonNull(amount, "Transaction amount is required");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "Balance after is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Timestamp is required");
        this.counterpartyAccountId = counterpartyAccountId;
    }

    public String id() {
        return id;
    }

    public TransactionType type() {
        return type;
    }

    public Money amount() {
        return amount;
    }

    public Money balanceAfter() {
        return balanceAfter;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Optional<AccountId> counterpartyAccountId() {
        return Optional.ofNullable(counterpartyAccountId);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
