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
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void open_account_makes_the_balance_queryable() {
        banking.openAccount(id("alice"), Money.of("100.00"));

        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("100.00"));
    }

    @Test
    void cannot_open_two_accounts_with_the_same_id() {
        banking.openAccount(id("alice"), Money.zero());

        assertThatThrownBy(() -> banking.openAccount(id("alice"), Money.of("1.00")))
                .isInstanceOf(DuplicateAccountException.class);
        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.zero());
    }

    @Test
    void deposit_and_withdraw_update_the_stored_balance() {
        banking.openAccount(id("alice"), Money.of("50.00"));

        banking.deposit(id("alice"), Money.of("20.00"));
        banking.withdraw(id("alice"), Money.of("15.00"));

        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("55.00"));
    }

    @Test
    void transfer_moves_money_between_existing_accounts() {
        banking.openAccount(id("alice"), Money.of("80.00"));
        banking.openAccount(id("bob"), Money.of("10.00"));

        banking.transfer(id("alice"), id("bob"), Money.of("25.00"));

        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("55.00"));
        assertThat(banking.balanceOf(id("bob"))).isEqualTo(Money.of("35.00"));
    }

    @Test
    void history_is_ordered_and_timestamped() {
        banking.openAccount(id("alice"), Money.of("40.00"));
        banking.openAccount(id("bob"), Money.zero());
        banking.withdraw(id("alice"), Money.of("5.00"));
        banking.transfer(id("alice"), id("bob"), Money.of("10.00"));

        List<Transaction> alice = banking.historyOf(id("alice"));
        assertThat(alice).extracting(Transaction::type).containsExactly(
                TransactionType.DEPOSIT,
                TransactionType.WITHDRAWAL,
                TransactionType.TRANSFER_OUT
        );
        assertThat(alice).allSatisfy(entry -> assertThat(entry.occurredAt()).isEqualTo(NOW));

        List<Transaction> bob = banking.historyOf(id("bob"));
        assertThat(bob).extracting(Transaction::type).containsExactly(TransactionType.TRANSFER_IN);
        assertThat(bob.get(0).counterpartyAccountId()).contains(id("alice"));
    }

    @Test
    void queries_fail_when_the_account_does_not_exist() {
        assertThatThrownBy(() -> banking.balanceOf(id("missing")))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> banking.historyOf(id("missing")))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> banking.deposit(id("missing"), Money.of("1.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void transfer_is_rejected_when_either_account_is_missing() {
        banking.openAccount(id("alice"), Money.of("50.00"));

        assertThatThrownBy(() -> banking.transfer(id("alice"), id("bob"), Money.of("10.00")))
                .isInstanceOf(AccountNotFoundException.class);
        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("50.00"));

        assertThatThrownBy(() -> banking.transfer(id("bob"), id("alice"), Money.of("10.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void cannot_transfer_to_the_same_account() {
        banking.openAccount(id("alice"), Money.of("50.00"));

        assertThatThrownBy(() -> banking.transfer(id("alice"), id("alice"), Money.of("10.00")))
                .isInstanceOf(InvalidTransferException.class);
        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("50.00"));
        assertThat(banking.historyOf(id("alice"))).hasSize(1);
    }

    @Test
    void overdraft_on_transfer_leaves_both_accounts_unchanged() {
        banking.openAccount(id("alice"), Money.of("10.00"));
        banking.openAccount(id("bob"), Money.of("3.00"));

        assertThatThrownBy(() -> banking.transfer(id("alice"), id("bob"), Money.of("10.01")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
        assertThat(banking.balanceOf(id("bob"))).isEqualTo(Money.of("3.00"));
    }

    @Test
    void blank_account_ids_are_rejected() {
        assertThatThrownBy(() -> new AccountId("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fractional_cents_are_rejected_before_any_account_is_touched() {
        banking.openAccount(id("alice"), Money.of("10.00"));

        assertThatThrownBy(() -> banking.deposit(id("alice"), Money.of("1.001")))
                .isInstanceOf(InvalidAmountException.class);
        assertThat(banking.balanceOf(id("alice"))).isEqualTo(Money.of("10.00"));
    }

    private static AccountId id(String value) {
        return new AccountId(value);
    }
}
