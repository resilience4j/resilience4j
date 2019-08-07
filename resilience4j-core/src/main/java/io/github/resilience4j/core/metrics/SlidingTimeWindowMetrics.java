package io.github.resilience4j.core.metrics;


import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Metrics} implementation is backed by a sliding time window that aggregates only the calls made
 * in the last {@code N} seconds.
 *
 * The sliding time window is implemented with a circular array of {@code N} buckets.
 * If the time window size is 10 seconds, the circular array has always 10 buckets. Every bucket
 * stores the outcome of all calls which happen in a certain epoch second. The head bucket of the circular array stores the call outcomes of the
 * current epoch second. The other buckets stored the call outcomes of the previous {@code N-1} epoch seconds.
 *
 * The sliding window does not store call outcomes (tuples) individually, but incrementally updates partial aggregations (bucket) and a total aggregation.
 * The total aggregation is updated incrementally when a new call outcome is recorded. When the oldest bucket is evicted, the partial aggregation of that bucket
 * is subtracted from the total aggregation and the bucket is reset. (Subtract-on-Evict)
 *
 * The time to retrieve a Snapshot is constant 0(1), since the Snapshot is pre-aggregated and is independent of the time window size.
 * The space requirement (memory consumption) of this implementation should be nearly constant O(n), since the call outcomes (tuples) are not stored individually.
 * Only {@code N} partial aggregations and 1 total aggregation are created.
 */
public class SlidingTimeWindowMetrics implements Metrics {

    private final int timeWindowSizeInSeconds;
    private final Aggregation aggregation;
    final Bucket[] buckets;
    private final Clock clock;
    int headBucketIndex;

    /**
     * Creates a new {@link SlidingTimeWindowMetrics} with the given window of time.
     *
     * @param timeWindowSizeInSeconds     the window time in seconds
     */
    public SlidingTimeWindowMetrics(short timeWindowSizeInSeconds) {
        this(timeWindowSizeInSeconds, Clock.systemUTC());
    }

    /**
     * Creates a new {@link SlidingTimeWindowMetrics} with the given clock and window of time.
     *
     * @param timeWindowSizeInSeconds     the window time size in seconds
     * @param clock      the {@link Clock} to use
     */
    public SlidingTimeWindowMetrics(int timeWindowSizeInSeconds, Clock clock) {
        this.clock = clock;
        this.timeWindowSizeInSeconds = timeWindowSizeInSeconds;
        this.buckets = new Bucket[timeWindowSizeInSeconds];
        this.headBucketIndex = 0;
        long epochSecond = clock.instant().getEpochSecond();
        for (int i = 0; i < timeWindowSizeInSeconds; i++)
        {
            buckets[i] = new Bucket(epochSecond);
            epochSecond++;
        }
        this.aggregation = new Aggregation();
    }



    @Override
    public synchronized Snapshot record(long duration, TimeUnit durationUnit, Outcome outcome) {
        aggregation.record(duration, durationUnit, outcome);
        moveWindowToCurrentEpochSecond(getLatestBucket()).record(duration, durationUnit, outcome);
        return new SnapshotImpl(aggregation);
    }

    public synchronized Snapshot getSnapshot(){
        moveWindowToCurrentEpochSecond(getLatestBucket());
        return new SnapshotImpl(aggregation);
    }

    /**
     * Moves the end of the time window to the current epoch second.
     * The latest bucket of the circular array is used to calculate how many seconds the window must be moved.
     * The difference is calculated by subtracting the epoch second from the latest bucket from the current epoch second.
     * If the difference is greater than the time window size, the time window size is used.
     * 
     * @param latestBucket the latest bucket of the circular array
     */
    private Bucket moveWindowToCurrentEpochSecond(Bucket latestBucket) {
        long currentEpochSecond = clock.instant().getEpochSecond();
        long differenceInSeconds = currentEpochSecond - latestBucket.getEpochSecond();
        if(differenceInSeconds == 0){
            return latestBucket;
        }
        long secondsToMoveTheWindow = Math.min(differenceInSeconds, timeWindowSizeInSeconds);
        Bucket currentBucket;
        do{
            secondsToMoveTheWindow--;
            moveHeadIndexToNextBucket();
            currentBucket = getLatestBucket();
            aggregation.removeBucket(currentBucket);
            currentBucket.resetBucket(currentEpochSecond - secondsToMoveTheWindow);
        } while(secondsToMoveTheWindow > 0);
        return currentBucket;
    }

    /**
     * Returns the head bucket of the circular array.
     *
     * @return the head bucket of the circular array
     */
    Bucket getLatestBucket(){
        return buckets[headBucketIndex];
    }

    /**
     * Moves the headBucketIndex to the next bucket.
     *
     */
    void moveHeadIndexToNextBucket(){
        this.headBucketIndex = (headBucketIndex + 1) % timeWindowSizeInSeconds;
    }
}