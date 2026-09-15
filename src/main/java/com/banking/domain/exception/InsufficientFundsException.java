package com.banking.domain.exception;

import com.banking.domain.AccountId;
import com.banking.domain.Money;

public final class InsufficientFundsException extends BankingException {
    public InsufficientFundsException(AccountId accountId, Money requested, Money available) {
        super("Account " + accountId + " cannot debit " + requested + "; available balance is " + available);
    }
}
