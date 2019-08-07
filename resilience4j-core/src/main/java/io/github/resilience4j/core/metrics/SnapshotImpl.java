package io.github.resilience4j.core.metrics;

public class SnapshotImpl implements Snapshot {

    private long totalDurationInMillis;
    private int totalNumberOfSlowCalls;
    private int totalNumberOfFailedCalls;
    private int totalNumberOfCalls;

    SnapshotImpl(Aggregation aggregation) {
        this.totalDurationInMillis = aggregation.getTotalDurationInMillis();
        this.totalNumberOfSlowCalls = aggregation.getTotalNumberOfSlowSuccessfulCalls();
        this.totalNumberOfFailedCalls = aggregation.getTotalNumberOfFailedCalls();
        this.totalNumberOfCalls = aggregation.getTotalNumberOfCalls();
    }

    @Override
    public long getTotalDurationInMillis() {
        return totalDurationInMillis;
    }

    @Override
    public int getTotalNumberOfSlowSuccessfulCalls() {
        return totalNumberOfSlowCalls;
    }

    @Override
    public float getSlowCallsPercentage() {
        if(totalNumberOfCalls == 0){
            return 0;
        }
        return totalNumberOfSlowCalls * 100.0f / totalNumberOfCalls;
    }

    @Override
    public int getTotalNumberOfSuccessfulCalls() {
        return totalNumberOfCalls - totalNumberOfFailedCalls;
    }

    @Override
    public int getTotalNumberOfFailedCalls() {
        return totalNumberOfFailedCalls;
    }

    @Override
    public int getTotalNumberOfCalls() {
        return totalNumberOfCalls;
    }

    @Override
    public float getFailureRatePercentage() {
        if(totalNumberOfCalls == 0){
            return 0;
        }
        return totalNumberOfFailedCalls * 100.0f / totalNumberOfCalls;
    }

    @Override
    public long getAverageDurationInMillis() {
        if(totalNumberOfCalls == 0){
            return 0;
        }
        return totalDurationInMillis / totalNumberOfCalls;
    }
}
