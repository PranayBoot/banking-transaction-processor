package com.banking.domain.exception;

import com.banking.domain.AccountId;

public final class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(AccountId accountId) {
        super("Account not found: " + accountId);
    }
}
