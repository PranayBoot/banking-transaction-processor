package com.banking.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountIdTest {

    @Test
    void trims_whitespace_and_compares_by_value() {
        assertThat(new AccountId(" alice ")).isEqualTo(new AccountId("alice"));
        assertThat(new AccountId("a")).isLessThan(new AccountId("b"));
    }

    @Test
    void rejects_missing_values() {
        assertThatThrownBy(() -> new AccountId(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccountId(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
