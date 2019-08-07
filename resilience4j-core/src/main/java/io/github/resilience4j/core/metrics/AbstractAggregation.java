package io.github.resilience4j.core.metrics;

import java.util.concurrent.TimeUnit;

class AbstractAggregation {

    long totalDurationInMillis = 0;
    int numberOfSlowCalls = 0;
    int numberOfFailedCalls = 0;
    int numberOfCalls = 0;

    void record(long duration, TimeUnit durationUnit, Metrics.Outcome outcome){
        this.numberOfCalls++;
        this.totalDurationInMillis += durationUnit.toMillis(duration);
        switch (outcome)
        {
            case SLOW_SUCCESS: numberOfSlowCalls++;
                break;

            case SLOW_ERROR: numberOfSlowCalls++; numberOfFailedCalls++;
                break;

            case ERROR: numberOfFailedCalls++;
                break;
        }
    }
}
