package com.banking.infrastructure;

import com.banking.domain.Account;
import com.banking.domain.AccountId;
import com.banking.domain.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public boolean exists(AccountId id) {
        return accounts.containsKey(id);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
        accounts.put(account.id(), account);
    }
}
