package io.github.resilience4j.core.metrics;

import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.core.metrics.Metrics.*;

public class Bucket {

    private long epochSecond;
    long totalDurationInMillis = 0;
    int numberOfSlowSuccessfulCalls = 0;
    int numberOfFailedCalls = 0;
    int numberOfCalls = 0;

    Bucket(long epochSecond){
        this.epochSecond = epochSecond;
    }

    void record(long duration, TimeUnit durationUnit, Outcome outcome){
        this.numberOfCalls++;
        this.totalDurationInMillis += durationUnit.toMillis(duration);
        switch (outcome)
        {
            case SLOW_SUCCESS: numberOfSlowSuccessfulCalls++;
                break;

            case ERROR: numberOfFailedCalls++;
                break;
        }
    }

    void resetBucket(long epochSecond) {
        this.epochSecond = epochSecond;
        this.totalDurationInMillis = 0;
        this.numberOfSlowSuccessfulCalls = 0;
        this.numberOfFailedCalls = 0;
        this.numberOfCalls = 0;
    }

    public long getEpochSecond() {
        return epochSecond;
    }
}