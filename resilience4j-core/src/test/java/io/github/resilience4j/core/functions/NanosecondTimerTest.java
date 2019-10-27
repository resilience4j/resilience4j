package io.github.resilience4j.core.functions;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Class NanosecondTimerTest.
 */
public class NanosecondTimerTest {

    @Test
    public void shouldPassElapsedTimetoConsumer() {
        NanosecondTimer timer = NanosecondTimer.now();
        final long now = System.nanoTime();

        timer.elapsed((time) -> {
            long elapsed = (System.nanoTime() - now);

            //should be within .1 ms
            assertThat(time).isCloseTo(elapsed, within(100000l));
        });
    }

    @Test
    public void shouldPassElapsedTimetoConsumer2() {
        final long now = System.nanoTime();
        NanosecondTimer timer = NanosecondTimer.create(now);

        timer.elapsed((time) -> {
            long elapsed = (System.nanoTime() - now);

            //should be within .1 ms
            assertThat(time).isCloseTo(elapsed, within(100000l));
        });
    }

}