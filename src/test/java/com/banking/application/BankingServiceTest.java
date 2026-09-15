package com.banking.application;

import com.banking.domain.AccountId;
import com.banking.domain.Money;
import com.banking.domain.Transaction;
import com.banking.domain.TransactionType;
import com.banking.domain.exception.AccountNotFoundException;
import com.banking.domain.exception.DuplicateAccountException;
import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.exception.InvalidAmountException;
import com.banking.domain.exception.InvalidTransferException;
import com.banking.infrastructure.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Banking service")
class BankingServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");

    private BankingService banking;

    @BeforeEach
    void setUp() {
        banking = new BankingService(
                new InMemoryAccountRepository(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Nested
    @DisplayName("Open account")
    class OpenAccount {

        @Test
        @DisplayName("opens with a unique id and funded balance")
        void opens_with_unique_id_and_funded_balance() {
            banking.openAccount(id("alice"), Money.of("100.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("100.00"));
        }

        @Test
        @DisplayName("opens with a zero balance and an empty history")
        void opens_with_zero_balance_and_empty_history() {
            banking.openAccount(id("alice"), Money.zero());

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.zero());
            assertThat(banking.historyOf(id("alice"))).isEmpty();
        }

        @Test
        @DisplayName("records a funded opening as a deposit in the ledger")
        void records_funded_opening_as_deposit() {
            banking.openAccount(id("alice"), Money.of("40.00"));

            assertThat(banking.historyOf(id("alice"))).singleElement().satisfies(entry -> {
                assertThat(entry.type()).isEqualTo(TransactionType.DEPOSIT);
                assertThat(entry.amount()).isEqualTo(Money.of("40.00"));
                assertThat(entry.balanceAfter()).isEqualTo(Money.of("40.00"));
                assertThat(entry.occurredAt()).isEqualTo(NOW);
            });
        }

        @Test
        @DisplayName("rejects a duplicate account id and leaves the original untouched")
        void rejects_duplicate_id() {
            banking.openAccount(id("alice"), Money.of("10.00"));

            assertThatThrownBy(() -> banking.openAccount(id("alice"), Money.of("99.00")))
                    .isInstanceOf(DuplicateAccountException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
        }
    }

    @Nested
    @DisplayName("Deposit")
    class Deposit {

        @Test
        @DisplayName("increases the stored balance")
        void increases_balance() {
            banking.openAccount(id("alice"), Money.of("50.00"));

            banking.deposit(id("alice"), Money.of("20.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("70.00"));
        }

        @Test
        @DisplayName("appends a timestamped deposit to history")
        void appends_timestamped_history() {
            banking.openAccount(id("alice"), Money.zero());

            banking.deposit(id("alice"), Money.of("12.34"));

            Transaction deposit = banking.historyOf(id("alice")).get(0);
            assertThat(deposit.type()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(deposit.amount()).isEqualTo(Money.of("12.34"));
            assertThat(deposit.occurredAt()).isEqualTo(NOW);
            assertThat(deposit.counterpartyAccountId()).isEmpty();
        }

        @Test
        @DisplayName("rejects a missing account")
        void rejects_missing_account() {
            assertThatThrownBy(() -> banking.deposit(id("missing"), Money.of("1.00")))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("rejects a zero amount")
        void rejects_zero_amount() {
            banking.openAccount(id("alice"), Money.of("10.00"));

            assertThatThrownBy(() -> banking.deposit(id("alice"), Money.zero()))
                    .isInstanceOf(InvalidAmountException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
        }

        @Test
        @DisplayName("rejects more than two decimal places before touching the account")
        void rejects_fractional_cents() {
            banking.openAccount(id("alice"), Money.of("10.00"));

            assertThatThrownBy(() -> banking.deposit(id("alice"), Money.of("1.001")))
                    .isInstanceOf(InvalidAmountException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
        }
    }

    @Nested
    @DisplayName("Withdraw")
    class Withdraw {

        @Test
        @DisplayName("decreases the stored balance when funds are available")
        void decreases_balance() {
            banking.openAccount(id("alice"), Money.of("50.00"));

            banking.withdraw(id("alice"), Money.of("15.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("35.00"));
        }

        @Test
        @DisplayName("allows withdrawing the exact remaining balance")
        void allows_exact_balance() {
            banking.openAccount(id("alice"), Money.of("20.00"));

            banking.withdraw(id("alice"), Money.of("20.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.zero());
        }

        @Test
        @DisplayName("rejects an overdraft and leaves balance and history unchanged")
        void rejects_overdraft() {
            banking.openAccount(id("alice"), Money.of("10.00"));

            assertThatThrownBy(() -> banking.withdraw(id("alice"), Money.of("10.01")))
                    .isInstanceOf(InsufficientFundsException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
            assertThat(banking.historyOf(id("alice"))).hasSize(1);
        }

        @Test
        @DisplayName("rejects a missing account")
        void rejects_missing_account() {
            assertThatThrownBy(() -> banking.withdraw(id("missing"), Money.of("1.00")))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("rejects a zero amount")
        void rejects_zero_amount() {
            banking.openAccount(id("alice"), Money.of("10.00"));

            assertThatThrownBy(() -> banking.withdraw(id("alice"), Money.zero()))
                    .isInstanceOf(InvalidAmountException.class);
        }
    }

    @Nested
    @DisplayName("Transfer")
    class Transfer {

        @Test
        @DisplayName("moves money between two existing accounts")
        void moves_money_between_accounts() {
            banking.openAccount(id("alice"), Money.of("80.00"));
            banking.openAccount(id("bob"), Money.of("10.00"));

            banking.transfer(id("alice"), id("bob"), Money.of("25.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("55.00"));
            assertThat(banking.balanceOf(id("bob"))).isEqualTo(Money.of("35.00"));
        }

        @Test
        @DisplayName("allows transferring the exact source balance")
        void allows_exact_balance() {
            banking.openAccount(id("alice"), Money.of("15.00"));
            banking.openAccount(id("bob"), Money.zero());

            banking.transfer(id("alice"), id("bob"), Money.of("15.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.zero());
            assertThat(banking.balanceOf(id("bob"))).isEqualTo(Money.of("15.00"));
        }

        @Test
        @DisplayName("writes TRANSFER_OUT and TRANSFER_IN with the same timestamp and counterparties")
        void writes_both_ledgers() {
            banking.openAccount(id("alice"), Money.of("40.00"));
            banking.openAccount(id("bob"), Money.zero());

            banking.transfer(id("alice"), id("bob"), Money.of("10.00"));

            Transaction outgoing = last(banking.historyOf(id("alice")));
            assertThat(outgoing.type()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(outgoing.counterpartyAccountId()).contains(id("bob"));
            assertThat(outgoing.occurredAt()).isEqualTo(NOW);

            Transaction incoming = last(banking.historyOf(id("bob")));
            assertThat(incoming.type()).isEqualTo(TransactionType.TRANSFER_IN);
            assertThat(incoming.counterpartyAccountId()).contains(id("alice"));
            assertThat(incoming.occurredAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("rejects a missing destination and leaves the source unchanged")
        void rejects_missing_destination() {
            banking.openAccount(id("alice"), Money.of("50.00"));

            assertThatThrownBy(() -> banking.transfer(id("alice"), id("bob"), Money.of("10.00")))
                    .isInstanceOf(AccountNotFoundException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("50.00"));
        }

        @Test
        @DisplayName("rejects a missing source")
        void rejects_missing_source() {
            banking.openAccount(id("alice"), Money.of("50.00"));

            assertThatThrownBy(() -> banking.transfer(id("bob"), id("alice"), Money.of("10.00")))
                    .isInstanceOf(AccountNotFoundException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("50.00"));
        }

        @Test
        @DisplayName("rejects a transfer to the same account")
        void rejects_same_account() {
            banking.openAccount(id("alice"), Money.of("50.00"));

            assertThatThrownBy(() -> banking.transfer(id("alice"), id("alice"), Money.of("10.00")))
                    .isInstanceOf(InvalidTransferException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("50.00"));
            assertThat(banking.historyOf(id("alice"))).hasSize(1);
        }

        @Test
        @DisplayName("rejects an overdraft and leaves both accounts unchanged")
        void rejects_overdraft() {
            banking.openAccount(id("alice"), Money.of("10.00"));
            banking.openAccount(id("bob"), Money.of("3.00"));

            assertThatThrownBy(() -> banking.transfer(id("alice"), id("bob"), Money.of("10.01")))
                    .isInstanceOf(InsufficientFundsException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
            assertThat(banking.balanceOf(id("bob"))).isEqualTo(Money.of("3.00"));
        }

        @Test
        @DisplayName("rejects a zero amount")
        void rejects_zero_amount() {
            banking.openAccount(id("alice"), Money.of("10.00"));
            banking.openAccount(id("bob"), Money.zero());

            assertThatThrownBy(() -> banking.transfer(id("alice"), id("bob"), Money.zero()))
                    .isInstanceOf(InvalidAmountException.class);
            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
            assertThat(banking.balanceOf(id("bob"))).isEqualTo(Money.zero());
        }
    }

    @Nested
    @DisplayName("Query balance and history")
    class Queries {

        @Test
        @DisplayName("returns the current balance after mixed operations")
        void current_balance_after_mixed_operations() {
            banking.openAccount(id("alice"), Money.of("50.00"));
            banking.deposit(id("alice"), Money.of("20.00"));
            banking.withdraw(id("alice"), Money.of("15.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("55.00"));
        }

        @Test
        @DisplayName("returns history in the order operations happened")
        void history_is_ordered() {
            banking.openAccount(id("alice"), Money.of("40.00"));
            banking.openAccount(id("bob"), Money.zero());
            banking.withdraw(id("alice"), Money.of("5.00"));
            banking.transfer(id("alice"), id("bob"), Money.of("10.00"));

            assertThat(banking.historyOf(id("alice")))
                    .extracting(Transaction::type)
                    .containsExactly(
                            TransactionType.DEPOSIT,
                            TransactionType.WITHDRAWAL,
                            TransactionType.TRANSFER_OUT
                    );
        }

        @Test
        @DisplayName("keeps each account's ledger isolated")
        void ledgers_are_isolated() {
            banking.openAccount(id("alice"), Money.of("10.00"));
            banking.openAccount(id("bob"), Money.zero());
            banking.deposit(id("bob"), Money.of("3.00"));

            assertThat(banking.historyOf(id("alice"))).hasSize(1);
            assertThat(banking.historyOf(id("bob")))
                    .extracting(Transaction::type)
                    .containsExactly(TransactionType.DEPOSIT);
        }

        @Test
        @DisplayName("rejects balance and history queries for a missing account")
        void rejects_missing_account() {
            assertThatThrownBy(() -> banking.balanceOf(id("missing")))
                    .isInstanceOf(AccountNotFoundException.class);
            assertThatThrownBy(() -> banking.historyOf(id("missing")))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("treats ids that differ only by surrounding whitespace as the same account")
        void trims_account_ids() {
            banking.openAccount(id(" alice "), Money.of("5.00"));

            assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("5.00"));
        }
    }

    private static AccountId id(String value) {
        return new AccountId(value);
    }

    private static Transaction last(List<Transaction> history) {
        return history.get(history.size() - 1);
    }
}
