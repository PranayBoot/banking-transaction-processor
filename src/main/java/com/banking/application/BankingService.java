package com.banking.application;

import com.banking.domain.Account;
import com.banking.domain.AccountId;
import com.banking.domain.AccountRepository;
import com.banking.domain.Money;
import com.banking.domain.Transaction;
import com.banking.domain.exception.AccountNotFoundException;
import com.banking.domain.exception.DuplicateAccountException;
import com.banking.domain.exception.InvalidTransferException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Application API for the kata. Multi-account work (open, transfer) lives here;
 * single-account rules stay on {@link Account}.
 */
public final class BankingService {
    private final AccountRepository accounts;
    private final Clock clock;

    public BankingService(AccountRepository accounts, Clock clock) {
        this.accounts = Objects.requireNonNull(accounts, "Account repository is required");
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    public void openAccount(AccountId id, Money openingBalance) {
        if (accounts.exists(id)) {
            throw new DuplicateAccountException(id);
        }
        Account account = Account.open(id, openingBalance, now());
        accounts.save(account);
    }

    public Money balanceOf(AccountId id) {
        return requireAccount(id).balance();
    }

    public List<Transaction> historyOf(AccountId id) {
        return requireAccount(id).history();
    }

    public void deposit(AccountId id, Money amount) {
        Account account = requireAccount(id);
        account.deposit(amount, now());
        accounts.save(account);
    }

    public void withdraw(AccountId id, Money amount) {
        Account account = requireAccount(id);
        account.withdraw(amount, now());
        accounts.save(account);
    }

    public void transfer(AccountId sourceId, AccountId destinationId, Money amount) {
        if (sourceId.equals(destinationId)) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }
        Account source = requireAccount(sourceId);
        Account destination = requireAccount(destinationId);
        source.transferTo(destination, amount, now());
        accounts.save(source);
        accounts.save(destination);
    }

    private Account requireAccount(AccountId id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private Instant now() {
        return clock.instant();
    }
}
