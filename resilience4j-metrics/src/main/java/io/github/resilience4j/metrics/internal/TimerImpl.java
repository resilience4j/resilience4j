package io.github.resilience4j.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import io.github.resilience4j.metrics.Timer;

import static com.codahale.metrics.MetricRegistry.name;
import static com.codahale.metrics.Timer.*;

public class TimerImpl implements Timer, Timer.Metrics {

    private final String timerName;
    private final MetricRegistry metricRegistry;
    private com.codahale.metrics.Timer successfulCallsTimer;
    private com.codahale.metrics.Counter totalCallsCounter;
    private com.codahale.metrics.Counter failedCallsCounter;

    public TimerImpl(String timerName, MetricRegistry metricRegistry){
        this.timerName = timerName;
        this.metricRegistry = metricRegistry;
        this.successfulCallsTimer = metricRegistry.timer(name(timerName, "successful"));
        this.totalCallsCounter = metricRegistry.counter(name(timerName, "total"));
        this.failedCallsCounter = metricRegistry.counter(name(timerName, "failed"));
    }


    @Override
    public Context time() {
        totalCallsCounter.inc();
        return successfulCallsTimer.time();
    }

    @Override
    public void onError(Context context) {
        failedCallsCounter.inc();
    }

    @Override
    public void onSuccess(Context context) {
        context.stop();
    }

    @Override
    public String getName() {
        return timerName;
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @Override
    public Metrics getMetrics() {
        return this;
    }

    @Override
    public long getNumberOfTotalCalls() {
        return totalCallsCounter.getCount();
    }

    @Override
    public long getNumberOfSuccessfulCalls() {
        return successfulCallsTimer.getCount();
    }

    @Override
    public long getNumberOfFailedCalls() {
        return failedCallsCounter.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return successfulCallsTimer.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return successfulCallsTimer.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return successfulCallsTimer.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return successfulCallsTimer.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return successfulCallsTimer.getSnapshot();
    }
}
