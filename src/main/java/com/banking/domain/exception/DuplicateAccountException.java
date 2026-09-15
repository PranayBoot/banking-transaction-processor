package com.banking.domain.exception;

import com.banking.domain.AccountId;

public final class DuplicateAccountException extends BankingException {
    public DuplicateAccountException(AccountId accountId) {
        super("Account already exists: " + accountId);
    }
}
