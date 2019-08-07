package io.github.resilience4j.core.metrics;

class TotalAggregation extends AbstractAggregation{

    void removeBucket(PartialAggregation bucket){
        this.totalDurationInMillis -= bucket.totalDurationInMillis;
        this.numberOfSlowCalls -= bucket.numberOfSlowCalls;
        this.numberOfFailedCalls -= bucket.numberOfFailedCalls;
        this.numberOfCalls -= bucket.numberOfCalls;
    }
}
