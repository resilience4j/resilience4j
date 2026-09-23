package io.github.resilience4j.hedge.internal;

import io.github.resilience4j.hedge.event.HedgeEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AverageDurationSupplierTest {

    private static final Duration MIN_HEDGE_DELAY = Duration.ofMillis(1);

    @Test
    public void coldWindowWithZeroFactorReturnsMinDelayNotZero() {
        // factor == 0, empty metrics window -> getAverageResponseTime() is Duration.ZERO
        AverageDurationSupplier supplier = new AverageDurationSupplier(false, 0, true, 100);

        Duration result = supplier.get();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(MIN_HEDGE_DELAY);
        assertThat(result).isGreaterThan(Duration.ZERO);
    }

    @Test
    public void coldWindowWithPercentageFactorReturnsMinDelayNotZero() {
        AverageDurationSupplier supplier = new AverageDurationSupplier(true, 50, true, 100);

        Duration result = supplier.get();

        assertThat(result).isEqualTo(MIN_HEDGE_DELAY);
    }

    @Test
    public void coldWindowWithMillisFactorAddsFactorOnTopOfZeroAverage() {
        // average is zero, but factor=20ms is added on top -> result should be 20ms, not clamped
        AverageDurationSupplier supplier = new AverageDurationSupplier(false, 20, true, 100);

        Duration result = supplier.get();

        assertThat(result).isEqualTo(Duration.ofMillis(20));
        assertThat(result).isGreaterThan(MIN_HEDGE_DELAY);
    }

    @Test
    public void warmWindowReturnsActualAverageWhenAboveMinDelay() {
        AverageDurationSupplier supplier = new AverageDurationSupplier(false, 0, true, 100);
        supplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, Duration.ofMillis(50));
        supplier.accept(HedgeEvent.Type.PRIMARY_SUCCESS, Duration.ofMillis(60));

        Duration result = supplier.get();

        assertThat(result).isEqualTo(Duration.ofMillis(55));
    }

    @Test
    public void primaryFailureIsIgnoredWhenShouldMeasureErrorsIsFalse() {
        AverageDurationSupplier supplier = new AverageDurationSupplier(false, 0, false, 100);
        supplier.accept(HedgeEvent.Type.PRIMARY_FAILURE, Duration.ofMillis(200));

        Duration result = supplier.get();

        // failure not recorded -> window still cold -> clamped to min delay, not 200ms
        assertThat(result).isEqualTo(MIN_HEDGE_DELAY);
    }

    @Test
    public void primaryFailureIsRecordedWhenShouldMeasureErrorsIsTrue() {
        AverageDurationSupplier supplier = new AverageDurationSupplier(false, 0, true, 100);
        supplier.accept(HedgeEvent.Type.PRIMARY_FAILURE, Duration.ofMillis(200));

        Duration result = supplier.get();

        assertThat(result).isEqualTo(Duration.ofMillis(200));
    }
}