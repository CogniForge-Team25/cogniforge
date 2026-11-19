package com.cogniforge.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadinessCalculatorTest {

    @Test
    void halfSupportedWithNoGapsIsFiftyPercent() {
        assertThat(ReadinessCalculator.score(10, 5, 0)).isEqualTo(50.0);
    }

    @Test
    void eachOpenGapDeductsTwoPointFive() {
        assertThat(ReadinessCalculator.score(10, 5, 2)).isEqualTo(45.0);
    }

    @Test
    void fullySupportedWithNoGapsIsOneHundred() {
        assertThat(ReadinessCalculator.score(93, 93, 0)).isEqualTo(100.0);
    }

    @Test
    void scoreNeverGoesBelowZero() {
        assertThat(ReadinessCalculator.score(10, 1, 50)).isEqualTo(0.0);
    }

    @Test
    void scoreNeverGoesAboveOneHundred() {
        assertThat(ReadinessCalculator.score(10, 10, 0)).isLessThanOrEqualTo(100.0);
    }

    @Test
    void noRequiredControlsGivesZero() {
        assertThat(ReadinessCalculator.score(0, 0, 0)).isEqualTo(0.0);
    }

    @Test
    void negativeCountsAreRejected() {
        assertThatThrownBy(() -> ReadinessCalculator.score(10, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supportedCannotExceedRequired() {
        assertThatThrownBy(() -> ReadinessCalculator.score(10, 11, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
