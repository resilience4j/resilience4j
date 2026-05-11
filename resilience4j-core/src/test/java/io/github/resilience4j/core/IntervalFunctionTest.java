/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.core;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntervalFunctionTest {

    @Test
    void shouldRejectNonPositiveDuration() {
        final Duration negativeDuration = Duration.ofMillis(0);
        final Duration zeroDuration = Duration.ofMillis(0);
        final Duration positiveDuration = Duration.ofMillis(100);
        final long negativeInterval = -1;
        final long zeroInterval = 0;
        final long positiveInterval = 100;

        assertThatThrownBy(() -> IntervalFunction.of(negativeDuration)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.of(zeroDuration)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> IntervalFunction.of(negativeInterval)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.of(zeroInterval)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> IntervalFunction.ofExponentialBackoff(positiveDuration, 1, negativeDuration)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.ofExponentialBackoff(positiveDuration, 1, zeroDuration)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> IntervalFunction.ofExponentialBackoff(positiveInterval, 1, negativeInterval)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.ofExponentialBackoff(positiveInterval, 1, zeroInterval)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> IntervalFunction.ofExponentialRandomBackoff(positiveDuration, 1, negativeDuration)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.ofExponentialRandomBackoff(positiveDuration, 1, zeroDuration)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> IntervalFunction.ofExponentialRandomBackoff(positiveInterval, 1, negativeInterval)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.ofExponentialRandomBackoff(positiveInterval, 1, zeroInterval)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldPassPositiveDuration() {
        final List<Long> positiveIntervals = List.of(10L, 99L, 981L);
        final List<Duration> positiveDurations = List.of(
            Duration.ofMillis(10L), Duration.ofMillis(99L), Duration.ofMillis(981L)
        );

        positiveDurations.forEach(IntervalFunction::of);
        positiveIntervals.forEach(IntervalFunction::of);
        positiveDurations.forEach(IntervalFunction::ofRandomized);
        positiveIntervals.forEach(IntervalFunction::ofRandomized);
        positiveDurations.forEach(IntervalFunction::ofExponentialBackoff);
        positiveIntervals.forEach(IntervalFunction::ofExponentialBackoff);
        positiveDurations.forEach(IntervalFunction::ofExponentialRandomBackoff);
        positiveIntervals.forEach(IntervalFunction::ofExponentialRandomBackoff);
        positiveDurations.forEach(d -> IntervalFunction.ofExponentialBackoff(d, 1, d));
        positiveIntervals.forEach(i -> IntervalFunction.ofExponentialBackoff(i, 1, i));
        positiveDurations.forEach(d -> IntervalFunction.ofExponentialRandomBackoff(d, 1, d));
        positiveIntervals.forEach(i -> IntervalFunction.ofExponentialRandomBackoff(i, 1, i));

        assertThat(true).isTrue();
    }

    @Test
    void shouldRejectAttemptLessThenOne() {
        final List<IntervalFunction> fns = List.of(
            IntervalFunction.ofDefaults(),
            IntervalFunction.ofRandomized(),
            IntervalFunction.ofExponentialBackoff(),
            IntervalFunction.ofExponentialRandomBackoff()
        );

        for (IntervalFunction fn : fns) {
            assertThatThrownBy(() -> fn.apply(0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void shouldPassAttemptGreaterThenZero() {
        final List<IntervalFunction> fns = List.of(
            IntervalFunction.ofDefaults(),
            IntervalFunction.ofRandomized(),
            IntervalFunction.ofExponentialBackoff(),
            IntervalFunction.ofExponentialRandomBackoff()
        );

        for (IntervalFunction fn : fns) {
            fn.apply(1);
            fn.apply(2);
        }
    }

    @Test
    void shouldRejectOutOfBoundsRandomizationFactor() {
        final Duration duration = Duration.ofMillis(100);

        final float negativeFactor = -0.0001f;
        final float greaterThanOneFactor = 1.0001f;

        assertThatThrownBy(() -> IntervalFunction.ofRandomized(duration, negativeFactor)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IntervalFunction.ofRandomized(duration, greaterThanOneFactor)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldPassPositiveRandomizationFactor() {
        final Duration duration = Duration.ofMillis(100);
        final float multiplier = 1.5f;
        final List<Float> correctFactors = List.of(0.0f, 0.1f, 0.25f, 0.5f, 0.75f, 1.0f);

        correctFactors.forEach(v -> IntervalFunction.ofRandomized(duration, v));
        correctFactors.forEach(v -> IntervalFunction.ofExponentialRandomBackoff(duration, multiplier, v));

        assertThat(true).isTrue();
    }

    @Test
    void shouldPassPositiveMultiplier() {
        final Duration duration = Duration.ofMillis(100);
        final float greaterThanOneMultiplier = 1.0001f;

        IntervalFunction.ofExponentialBackoff(duration, greaterThanOneMultiplier);
        IntervalFunction.ofExponentialRandomBackoff(duration, greaterThanOneMultiplier);

        assertThat(true).isTrue();
    }

    @Test
    void generatesRandomizedIntervals() {
        final IntervalFunction f = IntervalFunction.ofRandomized(100, 0.5);

        for (int i = 1; i < 50; i++) {
            final long delay = f.apply(i);

            assertThat(delay).isGreaterThanOrEqualTo(50).isLessThanOrEqualTo(150);
        }
    }

    @Test
    void generatesExponentialIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialBackoff(100, 1.5);
        long prevV = f.apply(1);

        for (int i = 2; i < 50; i++) {
            final long v = f.apply(i);

            assertThat(v).isGreaterThan(prevV);
            prevV = v;
        }
    }

    @Test
    void generatesCappedExponentialIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialBackoff(100, 2, 100_000);
        long prevV = f.apply(1);

        for (int i = 2; i < 12; i++) {
            final long v = f.apply(i);

            assertThat(v).isGreaterThan(prevV);
            prevV = v;
        }

        assertThat(f.apply(12)).isEqualTo(100_000);
        assertThat(f.apply(13)).isEqualTo(100_000);
    }

    @Test
    void generatesExponentialRandomIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialRandomBackoff(100, 1.5, 0.5);
        long expectedV = 100;

        for (int i = 1; i < 50; i++) {
            final long v = f.apply(i);

            assertThat(v)
                .isGreaterThanOrEqualTo((long) (expectedV * 0.5) - 1)
                .isLessThanOrEqualTo((long) (expectedV * 1.5) + 1);
            expectedV = (long) (expectedV * 1.5);
        }
    }

    @Test
    void generatesCappedExponentialRandomIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialRandomBackoff(100, 1.5, 0.5,100_000);
        long expectedV = 100;

        for (int i = 1; i < 50; i++) {
            final long v = f.apply(i);

            long finalExpectedV = expectedV;

            assertThat(v)
                    .satisfiesAnyOf(
                x -> assertThat(x).isGreaterThanOrEqualTo((long) (finalExpectedV * 0.5) - 1),
                x -> assertThat(x).isEqualTo(100_000)
            )
                    .satisfiesAnyOf(
                x -> assertThat(x).isLessThanOrEqualTo((long) (finalExpectedV * 1.5) + 1),
                x -> assertThat(x).isEqualTo(100_000)
            );
            expectedV = (long) (expectedV * 1.5);
        }
    }
}
