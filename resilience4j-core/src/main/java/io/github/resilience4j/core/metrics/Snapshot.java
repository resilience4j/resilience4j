package io.github.resilience4j.core.metrics;

public interface Snapshot {

    /**
     * Returns the total duration of all calls in milliseconds.
     *
     * @return the total duration of all calls in milliseconds
     */
    long getTotalDurationInMillis();

    /**
     * Returns the total number of successful calls which were slower than a certain threshold.
     *
     * @return the total number of successful calls which were slower than a certain threshold
     */
    int getTotalNumberOfSlowSuccessfulCalls();

    /**
     * Returns the percentage of calls which were slower than a certain threshold.
     *
     * @return the percentage of call which were slower than a certain threshold
     */
    float getSlowCallsPercentage();

    /**
     * Returns the total number of successful calls.
     *
     * @return the total number of successful calls
     */
    int getTotalNumberOfSuccessfulCalls();

    /**
     * Returns the total number of failed calls.
     *
     * @return the total number of failed calls
     */
    int getTotalNumberOfFailedCalls();

    /**
     * Returns the total number of all calls.
     *
     * @return the total number of all calls
     */
    int getTotalNumberOfCalls();

    /**
     * Returns the throughput per second.
     *
     * @return the the call throughput per second.
     */
    float getAverageThroughputPerSecond();

    /**
     * Returns the failure rate in percentage.
     *
     * @return the failure rate in percentage
     */
    float getFailureRatePercentage();

    /**
     * Returns the average duration of all calls in milliseconds.
     *
     * @return the average duration of all calls in milliseconds
     */
    long getAverageDurationInMillis();
}
