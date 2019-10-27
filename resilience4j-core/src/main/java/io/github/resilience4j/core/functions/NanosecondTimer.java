package io.github.resilience4j.core.functions;


import java.util.function.LongConsumer;

/**
 * Class NanosecondTimer runs a consumer with elapsed time.
 */
public final class NanosecondTimer {
    private final long startNanos;

    /**
     * Creates an instance of NanosecondTimer
     *
     * @param startNanos start time to set initially.
     * @return instance of {@link NanosecondTimer}
     */
    public static NanosecondTimer create(long startNanos) {
        return new NanosecondTimer(startNanos);
    }

    /**
     * Creates an instance of NanosecondTimer with `System.nanoTime()` as initial start time.
     *
     * @return instance of {@link NanosecondTimer}
     */
    public static NanosecondTimer now() {
        return new NanosecondTimer(System.nanoTime());
    }

    private NanosecondTimer(long startNanos) {
        this.startNanos = startNanos;
    }

    /**
     * Run a consumer with elapsed time in Nanos.
     *
     * <pre><code>
     * NanosecondTimer timer = NanosecondTimer.now()
     * timer.elapsed((time) -&gt; { operation(time) })
     * </code></pre>
     *
     * @param consumer elapsed time is passed to {@link LongConsumer}.
     */
    public void elapsed(LongConsumer consumer) {
        consumer.accept(System.nanoTime() - startNanos);
    }
}