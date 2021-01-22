package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.event.AbstractBulkheadLimitEvent;
import org.junit.Before;
import org.junit.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * test the adoptive bulkhead limiter logic
 */
public class AdaptiveBulkheadGraphTest {

    private static final int SLOW_CALL_DURATION_THRESHOLD = 200;
    private AdaptiveBulkheadStateMachine bulkhead;
    private AdaptiveBulkheadConfig config;
    // enable if u need to see the graphs of the executions
    private boolean drawGraphs = !false;
    AtomicInteger count = new AtomicInteger();
    List<Double> time = new ArrayList<>();
    List<Integer> maxConcurrentCalls = new ArrayList<>();

    @Before
    public void setup() {
        config = AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(50)
            .minConcurrentCalls(5)
            .slidingWindowSize(5)
//            .slidingWindowTime(2)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .failureRateThreshold(50)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofMillis(SLOW_CALL_DURATION_THRESHOLD))
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
    }

    @Test
    public void testLimiter() {
        if (drawGraphs) {
            bulkhead.getEventPublisher().onLimitIncreased(this::recordLimitChange);
            bulkhead.getEventPublisher().onLimitDecreased(this::recordLimitChange);
        }
        // if u like to get the graphs, increase the number of iterations to have better distribution
        for (int i = 0; i < 3000; i++) {
            final Duration duration = Duration.ofMillis(randomLatency(5, 2 * SLOW_CALL_DURATION_THRESHOLD));
            bulkhead.onSuccess(duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        assertThat(config).isNotNull();
        assertThat(bulkhead).isNotNull();

        if (drawGraphs) {
            // Create Chart
            XYChart chart2 = new XYChartBuilder().width(800).height(600)
                .title(getClass().getSimpleName()).xAxisTitle("time")
                .yAxisTitle("Concurrency limit").build();
            chart2.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
            chart2.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
            chart2.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
            chart2.getStyler().setYAxisDecimalPattern("ConcurrentCalls #");
            chart2.getStyler().setPlotMargin(0);
            chart2.getStyler().setPlotContentSize(.95);

            chart2.addSeries("MaxConcurrentCalls", time, maxConcurrentCalls);
            try {
                BitmapEncoder
                    .saveJPGWithQuality(chart2, "./AdaptiveBulkheadConcurrency.jpg", 0.95f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public long randomLatency(int min, int max) {
        return min + ThreadLocalRandom.current().nextLong(max - min);
    }

    private void recordLimitChange(AbstractBulkheadLimitEvent event) {
        maxConcurrentCalls.add(Integer.parseInt(event.eventData().get("newMaxConcurrentCalls")));
        time.add((double) count.incrementAndGet());
    }
}