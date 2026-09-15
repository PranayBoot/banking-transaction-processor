package com.banking.domain;

import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account")
class AccountTest {

    private static final Instant T0 = Instant.parse("2026-09-16T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-09-16T00:01:00Z");

    @Nested
    @DisplayName("Opening")
    class Opening {

        @Test
        @DisplayName("opens with zero balance and an empty ledger")
        void zero_opening() {
            Account account = Account.open(id("acc-1"), Money.zero(), T0);

            assertThat(account.balance()).isEqualTo(Money.zero());
            assertThat(account.history()).isEmpty();
        }

        @Test
        @DisplayName("records a non-zero opening balance as a deposit")
        void funded_opening() {
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
        @DisplayName("rejects a null opening balance")
        void null_opening_balance() {
            assertThatThrownBy(() -> Account.open(id("acc-1"), null, T0))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("increases balance and appends a ledger entry")
        void increases_balance() {
            Account account = Account.open(id("acc-1"), Money.zero(), T0, ids("txn-1"));

            account.deposit(Money.of("10.00"), T1);

            assertThat(account.balance()).isEqualTo(Money.of("10.00"));
            Transaction deposit = account.history().get(0);
            assertThat(deposit.type()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(deposit.amount()).isEqualTo(Money.of("10.00"));
            assertThat(deposit.balanceAfter()).isEqualTo(Money.of("10.00"));
            assertThat(deposit.occurredAt()).isEqualTo(T1);
        }

        @Test
        @DisplayName("rejects zero or null amounts")
        void rejects_invalid_amounts() {
            Account account = funded("acc-1", "10.00");

            assertThatThrownBy(() -> account.deposit(Money.zero(), T1))
                    .isInstanceOf(InvalidAmountException.class);
            assertThatThrownBy(() -> account.deposit(null, T1))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    @DisplayName("Withdraw")
    class Withdraw {

        @Test
        @DisplayName("decreases balance when funds are available")
        void decreases_balance() {
            Account account = funded("acc-1", "40.00");

            account.withdraw(Money.of("15.50"), T1);

            assertThat(account.balance()).isEqualTo(Money.of("24.50"));
            Transaction withdrawal = account.history().get(1);
            assertThat(withdrawal.type()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(withdrawal.amount()).isEqualTo(Money.of("15.50"));
            assertThat(withdrawal.balanceAfter()).isEqualTo(Money.of("24.50"));
        }

        @Test
        @DisplayName("allows withdrawing the exact remaining balance")
        void exact_balance() {
            Account account = funded("acc-1", "10.00");

            account.withdraw(Money.of("10.00"), T1);

            assertThat(account.balance()).isEqualTo(Money.zero());
        }

        @Test
        @DisplayName("rejects an overdraft without recording a ledger entry")
        void overdraft() {
            Account account = funded("acc-1", "10.00");

            assertThatThrownBy(() -> account.withdraw(Money.of("10.01"), T1))
                    .isInstanceOf(InsufficientFundsException.class);
            assertThat(account.balance()).isEqualTo(Money.of("10.00"));
            assertThat(account.history()).hasSize(1);
        }

        @Test
        @DisplayName("rejects a zero amount")
        void zero_amount() {
            Account account = funded("acc-1", "10.00");

            assertThatThrownBy(() -> account.withdraw(Money.zero(), T1))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    @DisplayName("Transfer")
    class Transfer {

        @Test
        @DisplayName("moves funds and writes both ledgers")
        void moves_funds() {
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
        @DisplayName("does not credit the destination when the source cannot pay")
        void no_partial_credit() {
            Account source = funded("src", "5.00");
            Account destination = funded("dst", "1.00");

            assertThatThrownBy(() -> source.transferTo(destination, Money.of("5.01"), T1))
                    .isInstanceOf(InsufficientFundsException.class);
            assertThat(source.balance()).isEqualTo(Money.of("5.00"));
            assertThat(destination.balance()).isEqualTo(Money.of("1.00"));
            assertThat(destination.history()).hasSize(1);
        }

        @Test
        @DisplayName("rejects a missing destination")
        void null_destination() {
            Account source = funded("src", "5.00");

            assertThatThrownBy(() -> source.transferTo(null, Money.of("1.00"), T1))
                    .isInstanceOf(NullPointerException.class);
            assertThat(source.balance()).isEqualTo(Money.of("5.00"));
        }
    }

    @Test
    @DisplayName("callers cannot mutate the returned history")
    void history_is_immutable() {
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
