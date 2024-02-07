package io.github.resilience4j.core;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.LongStream;

import static io.github.resilience4j.core.IntervalFunctionCompanion.*;
import static java.util.Objects.requireNonNull;

/**
 * An IntervalFunction which can be used to calculate the wait interval. The input parameter of the
 * function is the number of attempts (attempt), the output parameter the wait interval in
 * milliseconds. The attempt parameter starts at 1 and increases with every further attempt.
 */
@FunctionalInterface
public interface IntervalFunction extends Function<Integer, Long> {

    long DEFAULT_INITIAL_INTERVAL = 500;
    double DEFAULT_MULTIPLIER = 1.5;
    double DEFAULT_RANDOMIZATION_FACTOR = 0.5;

    /**
     * Creates an IntervalFunction which returns a fixed default interval of 500 [ms].
     *
     * @return returns an IntervalFunction which returns a fixed default interval of 500 [ms]
     */
    static IntervalFunction ofDefaults() {
        return of(DEFAULT_INITIAL_INTERVAL);
    }

    static IntervalFunction of(long intervalMillis, Function<Long, Long> backoffFunction) {
        checkInterval(intervalMillis);
        requireNonNull(backoffFunction);

        return (attempt) -> {
            checkAttempt(attempt);
            return LongStream.iterate(intervalMillis, n -> backoffFunction.apply(n)).skip(attempt - 1L).findFirst().getAsLong();
        };
    }

    static IntervalFunction of(Duration interval, Function<Long, Long> backoffFunction) {
        return of(interval.toMillis(), backoffFunction);
    }


    /**
     * Creates an IntervalFunction which returns a fixed interval in milliseconds.
     *
     * @param intervalMillis the interval in milliseconds
     * @return an IntervalFunction which returns a fixed interval in milliseconds.
     */
    static IntervalFunction of(long intervalMillis) {
        checkInterval(intervalMillis);
        return attempt -> {
            checkAttempt(attempt);
            return intervalMillis;
        };
    }

    /**
     * Creates an IntervalFunction which returns a fixed interval specified by a given {@link Duration}.
     *
     * @param interval the interval
     * @return an IntervalFunction which returns a fixed interval specified by a given {@link Duration}.
     */
    static IntervalFunction of(Duration interval) {
        return of(interval.toMillis());
    }


    static IntervalFunction ofRandomized(long intervalMillis, double randomizationFactor) {
        checkInterval(intervalMillis);
        checkRandomizationFactor(randomizationFactor);
        return attempt -> {
            checkAttempt(attempt);
            return (long) randomize(intervalMillis, randomizationFactor);
        };
    }

    static IntervalFunction ofRandomized(Duration interval, double randomizationFactor) {
        return ofRandomized(interval.toMillis(), randomizationFactor);
    }

    static IntervalFunction ofRandomized(long intervalMillis) {
        return ofRandomized(intervalMillis, DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofRandomized(Duration interval) {
        return ofRandomized(interval.toMillis(), DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofRandomized() {
        return ofRandomized(DEFAULT_INITIAL_INTERVAL, DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofExponentialBackoff(long initialIntervalMillis, double multiplier, long maxIntervalMillis) {
        checkInterval(maxIntervalMillis);
        return attempt -> {
            checkAttempt(attempt);
            final long interval = ofExponentialBackoff(initialIntervalMillis, multiplier)
                .apply(attempt);
            return Math.min(interval, maxIntervalMillis);
        };
    }

    static IntervalFunction ofExponentialBackoff(Duration initialInterval, double multiplier, Duration maxInterval) {
        return ofExponentialBackoff(initialInterval.toMillis(), multiplier, maxInterval.toMillis());
    }

    static IntervalFunction ofExponentialBackoff(long initialIntervalMillis, double multiplier) {
        return of(initialIntervalMillis, x -> (long) (x * multiplier));
    }

    static IntervalFunction ofExponentialBackoff(Duration initialInterval, double multiplier) {
        return ofExponentialBackoff(initialInterval.toMillis(), multiplier);
    }

    static IntervalFunction ofExponentialBackoff(long initialIntervalMillis) {
        return ofExponentialBackoff(initialIntervalMillis, DEFAULT_MULTIPLIER);
    }

    static IntervalFunction ofExponentialBackoff(Duration initialInterval) {
        return ofExponentialBackoff(initialInterval.toMillis(), DEFAULT_MULTIPLIER);
    }

    static IntervalFunction ofExponentialBackoff() {
        return ofExponentialBackoff(DEFAULT_INITIAL_INTERVAL, DEFAULT_MULTIPLIER);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        long initialIntervalMillis,
        double multiplier,
        double randomizationFactor,
        long maxIntervalMillis
    ) {
        checkInterval(maxIntervalMillis);
        return attempt -> {
            checkAttempt(attempt);
            final long interval = ofExponentialRandomBackoff(initialIntervalMillis, multiplier, randomizationFactor)
                .apply(attempt);
            return Math.min(interval, maxIntervalMillis);
        };
    }

    static IntervalFunction ofExponentialRandomBackoff(
        long initialIntervalMillis,
        double multiplier,
        double randomizationFactor
    ) {
        checkInterval(initialIntervalMillis);
        checkRandomizationFactor(randomizationFactor);
        return attempt -> {
            checkAttempt(attempt);
            final long interval = of(initialIntervalMillis, x -> (long) (x * multiplier))
                .apply(attempt);
            return (long) randomize(interval, randomizationFactor);
        };
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval,
        double multiplier,
        double randomizationFactor,
        Duration maxInterval
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), multiplier,
            randomizationFactor, maxInterval.toMillis());
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval,
        double multiplier,
        double randomizationFactor
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), multiplier,
            randomizationFactor);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        long initialIntervalMillis,
        double multiplier,
        long maxIntervalMillis
    ) {
        return ofExponentialRandomBackoff(initialIntervalMillis, multiplier,
            DEFAULT_RANDOMIZATION_FACTOR, maxIntervalMillis);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        long initialIntervalMillis,
        double multiplier
    ) {
        return ofExponentialRandomBackoff(initialIntervalMillis, multiplier,
            DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval,
        double multiplier,
        Duration maxInterval
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), multiplier,
            DEFAULT_RANDOMIZATION_FACTOR, maxInterval.toMillis());
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval,
        double multiplier
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), multiplier,
            DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        long initialIntervalMillis
    ) {
        return ofExponentialRandomBackoff(initialIntervalMillis, DEFAULT_MULTIPLIER);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), DEFAULT_MULTIPLIER);
    }

    static IntervalFunction ofExponentialRandomBackoff() {
        return ofExponentialRandomBackoff(DEFAULT_INITIAL_INTERVAL, DEFAULT_MULTIPLIER,
            DEFAULT_RANDOMIZATION_FACTOR);
    }

}

final class IntervalFunctionCompanion {

    private IntervalFunctionCompanion() {
    }

    @SuppressWarnings("squid:S2245") // this is not security-sensitive code
    static double randomize(final double current, final double randomizationFactor) {
        final double delta = randomizationFactor * current;
        final double min = current - delta;
        final double max = current + delta;

        return (min + (Math.random() * (max - min + 1)));
    }

    static void checkInterval(long intervalMillis) {
        if (intervalMillis < 1) {
            throw new IllegalArgumentException(
                "Illegal argument interval: " + intervalMillis + " millis is less than 1");
        }
    }

    static void checkRandomizationFactor(double randomizationFactor) {
        if (randomizationFactor < 0.0 || randomizationFactor > 1.0) {
            throw new IllegalArgumentException(
                "Illegal argument randomizationFactor: " + randomizationFactor);
        }
    }

    static void checkAttempt(long attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("Illegal argument attempt: " + attempt);
        }
    }
}
