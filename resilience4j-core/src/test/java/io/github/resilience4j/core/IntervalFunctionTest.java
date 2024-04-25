package io.github.resilience4j.core;

import io.vavr.collection.List;
import io.vavr.control.Try;
import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

public class IntervalFunctionTest {

    @Test
    public void shouldRejectNonPositiveDuration() {
        final Duration negativeDuration = Duration.ofMillis(0);
        final Duration zeroDuration = Duration.ofMillis(0);
        final Duration positiveDuration = Duration.ofMillis(100);
        final long negativeInterval = -1;
        final long zeroInterval = 0;
        final long positiveInterval = 100;

        List<Try<IntervalFunction>> tries = List.of(
            Try.of(() -> IntervalFunction.of(negativeDuration)),
            Try.of(() -> IntervalFunction.of(zeroDuration)),

            Try.of(() -> IntervalFunction.of(negativeInterval)),
            Try.of(() -> IntervalFunction.of(zeroInterval)),

            Try.of(() -> IntervalFunction.ofExponentialBackoff(positiveDuration, 1, negativeDuration)),
            Try.of(() -> IntervalFunction.ofExponentialBackoff(positiveDuration, 1, zeroDuration)),

            Try.of(() -> IntervalFunction.ofExponentialBackoff(positiveInterval, 1, negativeInterval)),
            Try.of(() -> IntervalFunction.ofExponentialBackoff(positiveInterval, 1, zeroInterval)),

            Try.of(() -> IntervalFunction.ofExponentialRandomBackoff(positiveDuration, 1, negativeDuration)),
            Try.of(() -> IntervalFunction.ofExponentialRandomBackoff(positiveDuration, 1, zeroDuration)),

            Try.of(() -> IntervalFunction.ofExponentialRandomBackoff(positiveInterval, 1, negativeInterval)),
            Try.of(() -> IntervalFunction.ofExponentialRandomBackoff(positiveInterval, 1, zeroInterval))
        );

        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException))
            .isTrue();
    }

    @Test
    public void shouldPassPositiveDuration() {
        final List<Long> positiveIntervals = List.of(10L, 99L, 981L);
        final List<Duration> positiveDurations = positiveIntervals.map(Duration::ofMillis);

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
    public void shouldRejectAttemptLessThenOne() {
        final List<IntervalFunction> fns = List.of(
            IntervalFunction.ofDefaults(),
            IntervalFunction.ofRandomized(),
            IntervalFunction.ofExponentialBackoff(),
            IntervalFunction.ofExponentialRandomBackoff()
        );

        final List<Try<Long>> tries = fns.map(fn -> Try.of(() -> fn.apply(0)));

        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException))
            .isTrue();
    }

    @Test
    public void shouldPassAttemptGreaterThenZero() {
        final List<IntervalFunction> fns = List.of(
            IntervalFunction.ofDefaults(),
            IntervalFunction.ofRandomized(),
            IntervalFunction.ofExponentialBackoff(),
            IntervalFunction.ofExponentialRandomBackoff()
        );

        final List<Try<Long>> tries1 = fns.map(fn -> Try.of(() -> fn.apply(1)));
        final List<Try<Long>> tries2 = fns.map(fn -> Try.of(() -> fn.apply(2)));

        assertThat(tries1.forAll(Try::isFailure)).isFalse();
        assertThat(tries2.forAll(Try::isFailure)).isFalse();
    }

    @Test
    public void shouldRejectOutOfBoundsRandomizationFactor() {
        final Duration duration = Duration.ofMillis(100);

        final float negativeFactor = -0.0001f;
        final float greaterThanOneFactor = 1.0001f;

        final List<Try<IntervalFunction>> tries = List.of(
            Try.of(() -> IntervalFunction.ofRandomized(duration, negativeFactor)),
            Try.of(() -> IntervalFunction.ofRandomized(duration, greaterThanOneFactor))
        );

        assertThat(tries.forAll(Try::isFailure)).isTrue();
        assertThat(tries.map(Try::getCause).forAll(t -> t instanceof IllegalArgumentException))
            .isTrue();
    }

    @Test
    public void shouldPassPositiveRandomizationFactor() {
        final Duration duration = Duration.ofMillis(100);
        final float multiplier = 1.5f;
        final List<Float> correctFactors = List.of(0.0f, 0.1f, 0.25f, 0.5f, 0.75f, 1.0f);

        correctFactors.forEach(v -> IntervalFunction.ofRandomized(duration, v));
        correctFactors
            .forEach(v -> IntervalFunction.ofExponentialRandomBackoff(duration, multiplier, v));

        assertThat(true).isTrue();
    }

    @Test
    public void shouldPassPositiveMultiplier() {
        final Duration duration = Duration.ofMillis(100);
        final float greaterThanOneMultiplier = 1.0001f;

        IntervalFunction.ofExponentialBackoff(duration, greaterThanOneMultiplier);
        IntervalFunction.ofExponentialRandomBackoff(duration, greaterThanOneMultiplier);

        assertThat(true).isTrue();
    }

    @Test
    public void generatesRandomizedIntervals() {
        final IntervalFunction f = IntervalFunction.ofRandomized(100, 0.5);

        for (int i = 1; i < 50; i++) {
            final long delay = f.apply(i);

            assertThat(delay).isGreaterThanOrEqualTo(50).isLessThanOrEqualTo(150);
        }
    }

    @Test
    public void generatesExponentialIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialBackoff(100, 1.5);
        long prevV = f.apply(1);

        for (int i = 2; i < 50; i++) {
            final long v = f.apply(i);

            assertThat(v).isGreaterThan(prevV);
            prevV = v;
        }
    }

    @Test
    public void generatesCappedExponentialIntervals() {
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
    public void generatesExponentialRandomIntervals() {
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
    public void generatesCappedExponentialRandomIntervals() {
        final IntervalFunction f = IntervalFunction.ofExponentialRandomBackoff(100, 1.5, 0.5,100_000);
        long expectedV = 100;

        for (int i = 1; i < 50; i++) {
            final long v = f.apply(i);

            long finalExpectedV = expectedV;

            assertThat(v).satisfiesAnyOf(
                x -> assertThat(x).isGreaterThanOrEqualTo((long) (finalExpectedV * 0.5) - 1),
                x -> assertThat(x).isEqualTo(100_000)
            );

            assertThat(v).satisfiesAnyOf(
                x -> assertThat(x).isLessThanOrEqualTo((long) (finalExpectedV * 1.5) + 1),
                x -> assertThat(x).isEqualTo(100_000)
            );
            expectedV = (long) (expectedV * 1.5);
        }
    }
}
