package com.banking.domain;

import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final Instant T0 = Instant.parse("2026-09-16T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-09-16T00:01:00Z");

    @Test
    void opens_with_zero_balance_and_an_empty_ledger() {
        Account account = Account.open(id("acc-1"), Money.zero(), T0);

        assertThat(account.balance()).isEqualTo(Money.zero());
        assertThat(account.history()).isEmpty();
    }

    @Test
    void records_a_non_zero_opening_balance_as_a_deposit() {
        Account account = Account.open(id("acc-1"), Money.of("25.00"), T0, ids("txn-1"));

        assertThat(account.balance()).isEqualTo(Money.of("25.00"));
        assertThat(account.history()).singleElement().satisfies(entry -> {
            assertThat(entry.id()).isEqualTo("txn-1");
            assertThat(entry.type()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(entry.amount()).isEqualTo(Money.of("25.00"));
            assertThat(entry.balanceAfter()).isEqualTo(Money.of("25.00"));
            assertThat(entry.occurredAt()).isEqualTo(T0);
            assertThat(entry.counterpartyAccountId()).isEmpty();
        });
    }

    @Test
    void deposit_increases_balance_and_appends_a_ledger_entry() {
        Account account = Account.open(id("acc-1"), Money.zero(), T0, ids("txn-1"));

        account.deposit(Money.of("10.00"), T1);

        assertThat(account.balance()).isEqualTo(Money.of("10.00"));
        assertThat(account.history()).hasSize(1);
        Transaction deposit = account.history().get(0);
        assertThat(deposit.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(deposit.amount()).isEqualTo(Money.of("10.00"));
        assertThat(deposit.balanceAfter()).isEqualTo(Money.of("10.00"));
        assertThat(deposit.occurredAt()).isEqualTo(T1);
    }

    @Test
    void withdrawal_decreases_balance_when_funds_are_available() {
        Account account = funded("acc-1", "40.00");

        account.withdraw(Money.of("15.50"), T1);

        assertThat(account.balance()).isEqualTo(Money.of("24.50"));
        Transaction withdrawal = account.history().get(1);
        assertThat(withdrawal.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(withdrawal.amount()).isEqualTo(Money.of("15.50"));
        assertThat(withdrawal.balanceAfter()).isEqualTo(Money.of("24.50"));
    }

    @Test
    void withdrawal_is_rejected_when_it_would_overdraft() {
        Account account = funded("acc-1", "10.00");

        assertThatThrownBy(() -> account.withdraw(Money.of("10.01"), T1))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.balance()).isEqualTo(Money.of("10.00"));
        assertThat(account.history()).hasSize(1);
    }

    @Test
    void transfer_moves_funds_and_writes_both_ledgers() {
        Account source = funded("src", "100.00");
        Account destination = Account.open(id("dst"), Money.zero(), T0);

        source.transferTo(destination, Money.of("30.00"), T1);

        assertThat(source.balance()).isEqualTo(Money.of("70.00"));
        assertThat(destination.balance()).isEqualTo(Money.of("30.00"));

        Transaction outgoing = source.history().get(1);
        assertThat(outgoing.type()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(outgoing.counterpartyAccountId()).contains(id("dst"));
        assertThat(outgoing.balanceAfter()).isEqualTo(Money.of("70.00"));

        Transaction incoming = destination.history().get(0);
        assertThat(incoming.type()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(incoming.counterpartyAccountId()).contains(id("src"));
        assertThat(incoming.balanceAfter()).isEqualTo(Money.of("30.00"));
        assertThat(incoming.occurredAt()).isEqualTo(T1);
    }

    @Test
    void transfer_does_not_credit_the_destination_when_the_source_cannot_pay() {
        Account source = funded("src", "5.00");
        Account destination = funded("dst", "1.00");

        assertThatThrownBy(() -> source.transferTo(destination, Money.of("5.01"), T1))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(source.balance()).isEqualTo(Money.of("5.00"));
        assertThat(destination.balance()).isEqualTo(Money.of("1.00"));
        assertThat(destination.history()).hasSize(1);
    }

    @Test
    void operation_amounts_must_be_strictly_positive() {
        Account account = funded("acc-1", "10.00");

        assertThatThrownBy(() -> account.deposit(Money.zero(), T1))
                .isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> account.withdraw(Money.zero(), T1))
                .isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> account.deposit(null, T1))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void history_cannot_be_mutated_by_callers() {
        Account account = funded("acc-1", "10.00");

        assertThatThrownBy(() -> account.history().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Account funded(String accountId, String openingBalance) {
        return Account.open(id(accountId), Money.of(openingBalance), T0);
    }

    private static AccountId id(String value) {
        return new AccountId(value);
    }

    private static java.util.function.Supplier<String> ids(String... values) {
        AtomicInteger index = new AtomicInteger();
        return () -> values[index.getAndIncrement()];
    }
}
