package com.banking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account id")
class AccountIdTest {

    @Test
    @DisplayName("trims whitespace and compares by value")
    void trims_and_compares_by_value() {
        assertThat(new AccountId(" alice ")).isEqualTo(new AccountId("alice"));
        assertThat(new AccountId("a")).isLessThan(new AccountId("b"));
    }

    @Test
    @DisplayName("rejects null, empty, and blank values")
    void rejects_missing_values() {
        assertThatThrownBy(() -> new AccountId(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountId(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountId("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
