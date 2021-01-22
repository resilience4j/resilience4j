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

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * test the adoptive bulkhead limiter logic
 */
public class AdaptiveBulkheadGraphTest {

    private static final Random NON_RANDOM = new Random(0);
    private static final int SLOW_CALL_DURATION_THRESHOLD = 200;
    public static final int RATE_THRESHOLD = 50;
    public static final int MAX_CONCURRENT_CALLS = 100;
    private AdaptiveBulkheadStateMachine bulkhead;
    // TODO disable
    private boolean drawGraphs = !false;
    List<Double> time = new ArrayList<>();
    List<Integer> maxConcurrentCalls = new ArrayList<>();
    List<Float> callsRates = new ArrayList<>();

    @Before
    public void setup() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
            .minConcurrentCalls(2)
            .initialConcurrentCalls(10)
            .slidingWindowSize(5)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .failureRateThreshold(RATE_THRESHOLD)
            .slowCallRateThreshold(RATE_THRESHOLD)
            .slowCallDurationThreshold(Duration.ofMillis(SLOW_CALL_DURATION_THRESHOLD))
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onLimitIncreased(this::recordLimitChange);
        bulkhead.getEventPublisher().onLimitDecreased(this::recordLimitChange);
    }

    @Test
    public void testSlowCalls() {
        bulkhead.getEventPublisher().onLimitIncreased(this::recordSlowCallsRates);
        bulkhead.getEventPublisher().onLimitDecreased(this::recordSlowCallsRates);

        for (int i = 0; i < 800; i++) {
            bulkhead.onSuccess(nextLatency(), TimeUnit.MILLISECONDS);
        }
        drawGraph("testSlowCalls");
    }

    @Test
    public void testFailedCalls() {
        bulkhead.getEventPublisher().onLimitIncreased(this::recordFailureCallsRates);
        bulkhead.getEventPublisher().onLimitDecreased(this::recordFailureCallsRates);
        Throwable failure = new Throwable();

        for (int i = 0; i < 800; i++) {
            if (nextErrorOccurred()) {
                bulkhead.onError(1, TimeUnit.MILLISECONDS, failure);
            } else {
                bulkhead.onSuccess(1, TimeUnit.MILLISECONDS);
            }
        }
        drawGraph("testFailedCalls");
    }

    @Test
    public void testFailedCallsOnly() {
        bulkhead.getEventPublisher().onLimitIncreased(this::recordFailureCallsRates);
        bulkhead.getEventPublisher().onLimitDecreased(this::recordFailureCallsRates);
        Throwable failure = new Throwable();

        for (int i = 0; i < 800; i++) {
            bulkhead.onError(1, TimeUnit.MILLISECONDS, failure);
        }
        drawGraph("testFailedCallsOnly");
    }

    @Test
    public void testSuccessfulCallsOnly() {
        bulkhead.getEventPublisher().onLimitIncreased(this::recordFailureCallsRates);
        bulkhead.getEventPublisher().onLimitDecreased(this::recordFailureCallsRates);

        for (int i = 0; i < 800; i++) {
            bulkhead.onSuccess(1, TimeUnit.MILLISECONDS);
        }
        drawGraph("testSuccessfulCallsOnly");
    }

    private int nextLatency() {
        return NON_RANDOM.nextInt(2 * SLOW_CALL_DURATION_THRESHOLD);
    }

    private boolean nextErrorOccurred() {
        return NON_RANDOM.nextInt(2) == 0;
    }

    private void recordLimitChange(AbstractBulkheadLimitEvent event) {
        maxConcurrentCalls.add(bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        time.add((double) maxConcurrentCalls.size());
    }

    private void recordSlowCallsRates(AbstractBulkheadLimitEvent event) {
        callsRates.add((float) (bulkhead.getMetrics().getSlowCallRate() >=
            bulkhead.getBulkheadConfig().getSlowCallRateThreshold() ? MAX_CONCURRENT_CALLS : 0));
    }

    private void recordFailureCallsRates(AbstractBulkheadLimitEvent event) {
        callsRates.add((float) (bulkhead.getMetrics().getFailureRate() >=
            bulkhead.getBulkheadConfig().getFailureRateThreshold() ? MAX_CONCURRENT_CALLS : 0));
    }

    private void drawGraph(String testName) {
        if (!drawGraphs) {
            return;
        }
        XYChart chart2 = new XYChartBuilder().title(getClass().getSimpleName() + " - " + testName)
            .width(1600)
            .height(800)
            .xAxisTitle("time")
            .yAxisTitle("concurrency limit")
            .build();
        chart2.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart2.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        //noinspection SuspiciousNameCombination
        chart2.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
        chart2.getStyler().setPlotMargin(0);
        chart2.getStyler().setPlotContentSize(.95);

        chart2.addSeries("CallsRates", time, callsRates)
            .setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.StepArea)
            .setMarkerColor(Color.WHITE)
            .setLineColor(Color.ORANGE)
            .setFillColor(Color.ORANGE);
        chart2.addSeries("MaxConcurrentCalls", time, maxConcurrentCalls)
            .setMarkerColor(Color.CYAN)
            .setLineColor(Color.BLUE);
        try {
            BitmapEncoder
                .saveJPGWithQuality(chart2, "./" + testName + ".jpg", 0.95f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}