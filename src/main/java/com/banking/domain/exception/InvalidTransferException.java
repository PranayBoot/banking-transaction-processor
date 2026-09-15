package com.banking.domain.exception;

public final class InvalidTransferException extends BankingException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
