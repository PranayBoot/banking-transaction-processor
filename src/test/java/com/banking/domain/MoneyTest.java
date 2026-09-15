package com.banking.domain;

import com.banking.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void stores_amounts_at_cent_precision() {
        assertThat(Money.of("10")).isEqualTo(Money.of(new BigDecimal("10.00")));
        assertThat(Money.of("10.5").toString()).isEqualTo("10.50");
    }

    @Test
    void treats_equal_values_as_equal_regardless_of_scale() {
        assertThat(Money.of("1.5")).isEqualTo(Money.of("1.50"));
    }

    @Test
    void adds_and_subtracts_without_losing_cents() {
        Money sum = Money.of("10.10").plus(Money.of("0.05"));
        assertThat(sum).isEqualTo(Money.of("10.15"));
        assertThat(sum.minus(Money.of("0.15"))).isEqualTo(Money.of("10.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "-10", "1.001", "abc", ""})
    void rejects_invalid_amounts(String amount) {
        assertThatThrownBy(() -> Money.of(amount))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void rejects_null_amount() {
        assertThatThrownBy(() -> Money.of((String) null))
                .isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void refuses_to_go_negative_through_subtraction() {
        assertThatThrownBy(() -> Money.of("1.00").minus(Money.of("1.01")))
                .isInstanceOf(InvalidAmountException.class);
    }
}
