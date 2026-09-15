package com.banking.domain;

import com.banking.domain.exception.InvalidAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("stores amounts at cent precision")
    void stores_cent_precision() {
        assertThat(Money.of("10")).isEqualTo(Money.of(new BigDecimal("10.00")));
        assertThat(Money.of("10.5").toString()).isEqualTo("10.50");
    }

    @Test
    @DisplayName("trims whitespace around numeric strings")
    void trims_whitespace() {
        assertThat(Money.of("  2.50  ")).isEqualTo(Money.of("2.50"));
    }

    @Test
    @DisplayName("treats equal values as equal regardless of scale")
    void equality_ignores_scale() {
        assertThat(Money.of("1.5")).isEqualTo(Money.of("1.50"));
        assertThat(Money.of("1.5")).hasSameHashCodeAs(Money.of("1.50"));
    }

    @Test
    @DisplayName("adds and subtracts without losing cents")
    void arithmetic() {
        Money sum = Money.of("10.10").plus(Money.of("0.05"));
        assertThat(sum).isEqualTo(Money.of("10.15"));
        assertThat(sum.minus(Money.of("0.15"))).isEqualTo(Money.of("10.00"));
    }

    @Test
    @DisplayName("zero is not positive")
    void zero_is_not_positive() {
        assertThat(Money.zero().isZero()).isTrue();
        assertThat(Money.zero().isPositive()).isFalse();
        assertThat(Money.of("0.01").isPositive()).isTrue();
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = {"-0.01", "-10", "1.001", "abc"})
    @NullAndEmptySource
    @DisplayName("rejects invalid amounts")
    void rejects_invalid_amounts(String amount) {
        assertThatThrownBy(() -> Money.of(amount))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("rejects a blank string")
    void rejects_blank() {
        assertThatThrownBy(() -> Money.of("   "))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("rejects a null BigDecimal")
    void rejects_null_big_decimal() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("refuses to go negative through subtraction")
    void refuses_negative_subtraction() {
        assertThatThrownBy(() -> Money.of("1.00").minus(Money.of("1.01")))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    @DisplayName("rejects null operands for plus and minus")
    void rejects_null_operands() {
        assertThatThrownBy(() -> Money.of("1.00").plus(null))
                .isInstanceOf(InvalidAmountException.class);
        assertThatThrownBy(() -> Money.of("1.00").minus(null))
                .isInstanceOf(InvalidAmountException.class);
    }
}
