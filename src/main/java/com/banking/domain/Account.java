package com.banking.domain;

import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.exception.InvalidAmountException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Aggregate root for a single account. Balance only changes through the
 * operations below, and each successful mutation appends one ledger entry.
 */
public final class Account {
    private final AccountId id;
    private Money balance;
    private final List<Transaction> ledger = new ArrayList<>();
    private final Supplier<String> transactionIds;

    public static Account open(AccountId id, Money openingBalance, Instant openedAt) {
        return open(id, openingBalance, openedAt, () -> UUID.randomUUID().toString());
    }

    static Account open(AccountId id, Money openingBalance, Instant openedAt, Supplier<String> transactionIds) {
        Account account = new Account(id, Money.zero(), transactionIds);
        if (openingBalance == null) {
            throw new InvalidAmountException("Opening balance is required");
        }
        if (!openingBalance.isZero()) {
            account.deposit(openingBalance, openedAt);
        }
        return account;
    }

    private Account(AccountId id, Money balance, Supplier<String> transactionIds) {
        this.id = java.util.Objects.requireNonNull(id, "Account id is required");
        this.balance = java.util.Objects.requireNonNull(balance, "Balance is required");
        this.transactionIds = java.util.Objects.requireNonNull(transactionIds, "Transaction id source is required");
    }

    public AccountId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }

    public List<Transaction> history() {
        return Collections.unmodifiableList(ledger);
    }

    public void deposit(Money amount, Instant at) {
        credit(requirePositive(amount), TransactionType.DEPOSIT, at, null);
    }

    public void withdraw(Money amount, Instant at) {
        debit(requirePositive(amount), TransactionType.WITHDRAWAL, at, null);
    }

    public void transferTo(Account destination, Money amount, Instant at) {
        java.util.Objects.requireNonNull(destination, "Destination account is required");
        Money transferAmount = requirePositive(amount);
        debit(transferAmount, TransactionType.TRANSFER_OUT, at, destination.id());
        destination.credit(transferAmount, TransactionType.TRANSFER_IN, at, id);
    }

    private void debit(Money amount, TransactionType type, Instant at, AccountId counterparty) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(id, amount, balance);
        }
        balance = balance.minus(amount);
        record(type, amount, at, counterparty);
    }

    private void credit(Money amount, TransactionType type, Instant at, AccountId counterparty) {
        balance = balance.plus(amount);
        record(type, amount, at, counterparty);
    }

    private void record(TransactionType type, Money amount, Instant at, AccountId counterparty) {
        ledger.add(new Transaction(
                transactionIds.get(),
                type,
                amount,
                balance,
                java.util.Objects.requireNonNull(at, "Timestamp is required"),
                counterparty
        ));
    }

    private static Money requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidAmountException("Operation amount must be greater than zero");
        }
        return amount;
    }
}
