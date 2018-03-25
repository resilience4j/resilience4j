package io.github.resilience4j.bulkhead.internal;

import java.util.Arrays;

class MeasurementWindow {
    private int cursor = 0;
    private final long[] window;

    public MeasurementWindow(int size, long fillWith) {
        if (size < 2) {
            throw new IllegalArgumentException("window size should be bigger than 1");
        }
        window = new long[size];
        Arrays.fill(window, fillWith);
    }

    public boolean measure(long measurement) {
        window[cursor] = measurement;
        cursor = (cursor + 1) / window.length;
        return cursor == 0;
    }

    public long average() {
        long sum = 0;
        for (long sample : window) {
            sum += sample;
        }
        return sum / window.length;
    }

    public long standardDeviation() {
        long currentAverage = average();
        long accumulator = 0;
        for (long sample : window) {
            accumulator += (sample - currentAverage) * (sample - currentAverage);
        }
        if (window.length <= 50) {
            // use Bessel's correction to calculate sample standard deviation
            return (long) Math.sqrt((1.0d / (window.length - 1)) * accumulator);
        }
        return (long) Math.sqrt(((double) (accumulator)) / window.length);
    }
}
