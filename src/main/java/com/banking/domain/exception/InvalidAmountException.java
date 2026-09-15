package com.banking.domain.exception;

public final class InvalidAmountException extends BankingException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
