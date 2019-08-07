package io.github.resilience4j.core.metrics;

public class PartialAggregation extends AbstractAggregation {

    private long epochSecond;

    PartialAggregation(long epochSecond){
        this.epochSecond = epochSecond;
    }

    void resetBucket(long epochSecond) {
        this.epochSecond = epochSecond;
        this.totalDurationInMillis = 0;
        this.numberOfSlowCalls = 0;
        this.numberOfFailedCalls = 0;
        this.numberOfCalls = 0;
    }

    public long getEpochSecond() {
        return epochSecond;
    }
}