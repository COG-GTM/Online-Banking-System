package com.userfront.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void parsesPositiveAmount() {
        assertThat(MoneyUtil.parseAmount(" 10.50 ")).isEqualByComparingTo(new BigDecimal("10.50"));
    }

    @Test
    void rejectsNegativeAndZeroAmounts() {
        assertThatThrownBy(() -> MoneyUtil.parseAmount("-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtil.parseAmount("0")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonNumericAndOverPreciseAmounts() {
        assertThatThrownBy(() -> MoneyUtil.parseAmount("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtil.parseAmount("1.005")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MoneyUtil.parseAmount("")).isInstanceOf(IllegalArgumentException.class);
    }
}
