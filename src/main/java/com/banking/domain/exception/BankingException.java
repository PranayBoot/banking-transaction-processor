package com.banking.domain.exception;

/**
 * Base type for domain failures. Callers can catch this to handle any
 * business-rule violation without depending on a specific subtype.
 */
public abstract class BankingException extends RuntimeException {
    protected BankingException(String message) {
        super(message);
    }
}
