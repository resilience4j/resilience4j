package io.github.resilience4j.retry;

import io.vavr.collection.Stream;

import java.time.Duration;
import java.util.function.Function;

import static io.github.resilience4j.retry.IntervalFunctionCompanion.*;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface IntervalFunction extends Function<Integer, Long> {

    long DEFAULT_INITIAL_INTERVAL = 500;
    double DEFAULT_MULTIPLIER = 1.5;
    double DEFAULT_RANDOMIZATION_FACTOR = 0.5;

    static IntervalFunction ofDefaults() {
        return of(DEFAULT_INITIAL_INTERVAL);
    }

    static IntervalFunction of(long intervalMillis, Function<Long, Long> backoffFn) {
        checkInterval(intervalMillis);
        requireNonNull(backoffFn);

        return (attempt) -> {
            checkAttempt(attempt);
            return Stream.iterate(intervalMillis, backoffFn).get(attempt - 1);
        };
    }

    static IntervalFunction of(Duration interval, Function<Long, Long> backoffFn) {
        return of(interval.toMillis(), backoffFn);
    }

    static IntervalFunction of(long intervalMillis) {
        return of(intervalMillis, x -> x);
    }

    static IntervalFunction of(Duration interval) {
        return of(interval.toMillis());
    }


    static IntervalFunction ofRandomized(long intervalMillis, double randomizationFactor) {
        checkInterval(intervalMillis);
        checkRandomizationFactor(randomizationFactor);
        return (attempt) -> {
            checkAttempt(attempt);
            return (long) randomize(intervalMillis, randomizationFactor);
        };
    }

    static IntervalFunction ofRandomized(Duration interval, double randomizationFactor) {
        return ofRandomized(interval.toMillis(), randomizationFactor);
    }

    static IntervalFunction ofRandomized(long interval) {
        return ofRandomized(interval, DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofRandomized(Duration interval) {
        return ofRandomized(interval.toMillis(), DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofRandomized() {
        return ofRandomized(DEFAULT_INITIAL_INTERVAL, DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofExponentialBackoff(long initialIntervalMillis, double multiplier) {
        checkMultiplier(multiplier);
        return of(initialIntervalMillis, (x) -> (long) (x * multiplier));

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
        double randomizationFactor
    ) {
        checkInterval(initialIntervalMillis);
        checkMultiplier(multiplier);
        checkRandomizationFactor(randomizationFactor);
        return (attempt) -> {
            checkAttempt(attempt);
            final long interval = of(initialIntervalMillis, (x) -> (long) (x * multiplier)).apply(attempt);
            return (long) randomize(interval, randomizationFactor);
        };
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval,
        double multiplier,
        double randomizationFactor
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), multiplier, randomizationFactor);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        long initialIntervalMillis,
        double multiplier
    ) {
        return ofExponentialRandomBackoff(initialIntervalMillis, multiplier, DEFAULT_RANDOMIZATION_FACTOR);
    }

    static IntervalFunction ofExponentialRandomBackoff(
        Duration initialInterval,
        double multiplier
    ) {
        return ofExponentialRandomBackoff(initialInterval.toMillis(), multiplier, DEFAULT_RANDOMIZATION_FACTOR);
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
        return ofExponentialRandomBackoff(DEFAULT_INITIAL_INTERVAL, DEFAULT_MULTIPLIER, DEFAULT_RANDOMIZATION_FACTOR);
    }

}

final class IntervalFunctionCompanion {
    private IntervalFunctionCompanion() {
    }

    static double randomize(final double current, final double randomizationFactor) {
        final double delta = randomizationFactor * current;
        final double min = current - delta;
        final double max = current + delta;
        return (min + (Math.random() * (max - min + 1)));
    }

    static void checkInterval(long v) {
        if (v < 10) {
            throw new IllegalArgumentException("Illegal argument interval: " + v + " millis");
        }
    }

    static void checkMultiplier(double v) {
        if (v < 1.0) {
            throw new IllegalArgumentException("Illegal argument multiplier: " + v);
        }
    }

    static void checkRandomizationFactor(double v) {
        if (v < 0.0 || v >= 1.0) {
            throw new IllegalArgumentException("Illegal argument randomizationFactor: " + v);
        }
    }

    static void checkAttempt(long v) {
        if (v < 1) {
            throw new IllegalArgumentException("Illegal argument attempt: " + v);
        }
    }
}