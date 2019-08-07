package io.github.resilience4j.core.metrics;

import java.util.concurrent.TimeUnit;

class Aggregation {

    private long totalDurationInMillis = 0;
    private int totalNumberOfSlowSuccessfulCalls = 0;
    private int totalNumberOfFailedCalls = 0;
    private int totalNumberOfCalls = 0;

    void record(long duration, TimeUnit durationUnit, Metrics.Outcome outcome){
        this.totalNumberOfCalls++;
        this.totalDurationInMillis += durationUnit.toMillis(duration);
        switch (outcome)
        {
            case SLOW_SUCCESS: totalNumberOfSlowSuccessfulCalls++;
                break;

            case ERROR: totalNumberOfFailedCalls++;
                break;
        }
    }

    void removeBucket(Bucket bucket){
        this.totalDurationInMillis -= bucket.totalDurationInMillis;
        this.totalNumberOfSlowSuccessfulCalls -= bucket.numberOfSlowSuccessfulCalls;
        this.totalNumberOfFailedCalls -= bucket.numberOfFailedCalls;
        this.totalNumberOfCalls -= bucket.numberOfCalls;
    }

    long getTotalDurationInMillis() {
        return totalDurationInMillis;
    }

    int getTotalNumberOfSlowSuccessfulCalls() {
        return totalNumberOfSlowSuccessfulCalls;
    }

    int getTotalNumberOfFailedCalls() {
        return totalNumberOfFailedCalls;
    }

    int getTotalNumberOfCalls() {
        return totalNumberOfCalls;
    }

}
