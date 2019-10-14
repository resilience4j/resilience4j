package io.github.resilience4j.core;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class IntervalFunctionTest {

    @Test
    public void shouldRejectNonPositiveDuration() {
        // Given

        final Duration negativeDuration = Duration.ofMillis(0);
        final Duration zeroDuration = Duration.ofMillis(0);
        final Duration smallDuration = Duration.ofMillis(9);

        final long negativeInterval = -1;
        final long zeroInterval = 0;
        final long smallInterval = 9;

        // When
        List<Try> tries = List.of(
            Try.of(() -> IntervalFunction.of(negativeDuration)),
            Try.of(() -> IntervalFunction.of(zeroDuration)),
            Try.of(() -> IntervalFunction.of(smallDuration)),

            Try.of(() -> IntervalFunction.of(negativeInterval)),
            Try.of(() -> IntervalFunction.of(zeroInterval)),
            Try.of(() -> IntervalFunction.of(smallInterval))
        );

        // Then
        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException)).isTrue();
    }

    @Test
    public void shouldPassPositiveDuration() {
        // Given

        final List<Long> positiveIntervals = List.of(10L, 99L, 981L);
        final List<Duration> positiveDurations = positiveIntervals.map(Duration::ofMillis);

        // When
        positiveDurations.forEach(IntervalFunction::of);
        positiveIntervals.forEach(IntervalFunction::of);

        positiveDurations.forEach(IntervalFunction::ofRandomized);
        positiveIntervals.forEach(IntervalFunction::ofRandomized);

        positiveDurations.forEach(IntervalFunction::ofExponentialBackoff);
        positiveIntervals.forEach(IntervalFunction::ofExponentialBackoff);

        positiveDurations.forEach(IntervalFunction::ofExponentialRandomBackoff);
        positiveIntervals.forEach(IntervalFunction::ofExponentialRandomBackoff);

        // Then should pass
        assertThat(true).isTrue();
    }

    @Test
    public void shouldRejectAttemptLessThenOne() {
        // Given
        final List<IntervalFunction> fns = List.of(
                IntervalFunction.ofDefaults(),
                IntervalFunction.ofRandomized(),
                IntervalFunction.ofExponentialBackoff(),
                IntervalFunction.ofExponentialRandomBackoff()
        );

        // When
        final List<Try> tries = fns.map(fn -> Try.of(() -> fn.apply(0)));

        // Then
        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException)).isTrue();
    }

    @Test
    public void shouldPassAttemptGreaterThenZero() {
        // Given
        final List<IntervalFunction> fns = List.of(
                IntervalFunction.ofDefaults(),
                IntervalFunction.ofRandomized(),
                IntervalFunction.ofExponentialBackoff(),
                IntervalFunction.ofExponentialRandomBackoff()
        );

        // When
        final List<Try> tries1 = fns.map(fn -> Try.of(() -> fn.apply(1)));
        final List<Try> tries2 = fns.map(fn -> Try.of(() -> fn.apply(2)));

        // Then
        assertThat(tries1.forAll(Try::isFailure)).isFalse();
        assertThat(tries2.forAll(Try::isFailure)).isFalse();
    }

    @Test
    public void shouldRejectOutOfBoundsRandomizationFactor() {
        // Given

        final Duration duration = Duration.ofMillis(100);

        final float negativeFactor = -0.0001f;
        final float greaterThanOneFactor = 1.0001f;

        // When
        final List<Try> tries = List.of(
                Try.of(() -> IntervalFunction.ofRandomized(duration, negativeFactor)),
                Try.of(() -> IntervalFunction.ofRandomized(duration, greaterThanOneFactor))
        );

        // Then
        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException)).isTrue();
    }

    @Test
    public void shouldPassPositiveRandomizationFactor() {
        // Given

        final Duration duration = Duration.ofMillis(100);
        final float multiplier = 1.5f;

        final List<Float> correctFactors = List.of(0.0f, 0.25f, 0.5f, 0.75f, 0.1f);

        // When
        correctFactors.forEach(v -> IntervalFunction.ofRandomized(duration, v));
        correctFactors.forEach(v -> IntervalFunction.ofExponentialRandomBackoff(duration, multiplier, v));

        assertThat(true).isTrue();
    }

    @Test
    public void shouldRejectOutOfBoundsMultiplier() {
        // Given

        final Duration duration = Duration.ofMillis(100);

        final float lessThenOneMultiplier = 0.9999f;

        // When
        final List<Try> tries = List.of(
                Try.of(() -> IntervalFunction.ofExponentialBackoff(duration, lessThenOneMultiplier)),
                Try.of(() -> IntervalFunction.ofExponentialRandomBackoff(duration, lessThenOneMultiplier))
        );

        // Then
        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException)).isTrue();
    }

    @Test
    public void shouldPassPositiveMultiplier() {
        // Given

        final Duration duration = Duration.ofMillis(100);
        final float greaterThanOneMultiplier = 1.0001f;

        // When
        IntervalFunction.ofExponentialBackoff(duration, greaterThanOneMultiplier);
        IntervalFunction.ofExponentialRandomBackoff(duration, greaterThanOneMultiplier);

        assertThat(true).isTrue();
    }

    @Test
    public void generatesRandomizedIntervals() {
        final IntervalFunction f = IntervalFunction.ofRandomized(100, 0.5);

        for (int i = 1; i < 50; i++) {
            //When
            final long delay = f.apply(i);

            // Then
            assertThat(delay).isGreaterThanOrEqualTo(50).isLessThanOrEqualTo(150);
        }
    }

    @Test
    public void generatesExponentialIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialBackoff(100, 1.5);

        long prevV = f.apply(1);

        for (int i = 2; i < 50; i++) {
            //When
            final long v = f.apply(i);

            // Then
            assertThat(v).isGreaterThan(prevV);

            prevV = v;
        }
    }

    @Test
    public void generatesExponentialRandomIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialRandomBackoff(100, 1.5, 0.5);
        long expectedV = 100;

        for (int i = 1; i < 50; i++) {

            //When
            final long v = f.apply(i);

            // Then
            assertThat(v)
                    .isGreaterThanOrEqualTo( (long)(expectedV * 0.5) - 1)
                    .isLessThanOrEqualTo((long)(expectedV * 1.5) + 1);

            expectedV = (long) (expectedV * 1.5);
        }
    }
}
