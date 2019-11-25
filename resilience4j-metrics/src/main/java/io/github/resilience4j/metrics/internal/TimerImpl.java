package io.github.resilience4j.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import io.github.resilience4j.metrics.Timer;

import static com.codahale.metrics.MetricRegistry.name;

public class TimerImpl implements Timer {

    public static final String SUCCESSFUL = "successful";
    public static final String TOTAL = "total";
    public static final String FAILED = "failed";
    private final String timerName;
    private final MetricRegistry metricRegistry;
    private final TimerMetrics metrics;
    private com.codahale.metrics.Timer successfulCallsTimer;
    private com.codahale.metrics.Counter totalCallsCounter;
    private com.codahale.metrics.Counter failedCallsCounter;

    public TimerImpl(String timerName, MetricRegistry metricRegistry) {
        this.timerName = timerName;
        this.metricRegistry = metricRegistry;
        this.successfulCallsTimer = metricRegistry.timer(name(timerName, SUCCESSFUL));
        this.totalCallsCounter = metricRegistry.counter(name(timerName, TOTAL));
        this.failedCallsCounter = metricRegistry.counter(name(timerName, FAILED));
        this.metrics = new TimerMetrics();
    }


    @Override
    public Timer.Context context() {
        totalCallsCounter.inc();
        return new ContextImpl();
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
        return metrics;
    }

    public final class ContextImpl implements Timer.Context {

        com.codahale.metrics.Timer.Context timerContext;

        private ContextImpl() {
            timerContext = successfulCallsTimer.time();
        }

        @Override
        public void onError() {
            failedCallsCounter.inc();
        }

        @Override
        public void onSuccess() {
            timerContext.stop();
        }
    }


    private final class TimerMetrics implements Metrics {

        private TimerMetrics() {
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
}
