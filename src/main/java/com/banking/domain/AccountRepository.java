package com.banking.domain;

import java.util.Optional;

/**
 * Persistence port. The domain never depends on how accounts are stored.
 */
public interface AccountRepository {
    boolean exists(AccountId id);

    Optional<Account> findById(AccountId id);

    void save(Account account);
}
