package io.github.resilience4j.retry;


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

    static void checkInterval(long interval) {
        if (interval < 10) {
            throw new IllegalArgumentException(
                "Illegal argument interval: " + interval + " millis");
        }
    }

    static void checkMultiplier(double multiplier) {
        if (multiplier < 1.0) {
            throw new IllegalArgumentException("Illegal argument multiplier: " + multiplier);
        }
    }

    static void checkRandomizationFactor(double randomizationFactor) {
        if (randomizationFactor < 0.0 || randomizationFactor >= 1.0) {
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