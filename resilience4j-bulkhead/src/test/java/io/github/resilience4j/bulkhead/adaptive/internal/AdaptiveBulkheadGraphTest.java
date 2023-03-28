package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.event.AbstractAdaptiveBulkheadEvent;
import org.junit.Before;
import org.junit.Test;
import org.knowm.xchart.*;
import org.knowm.xchart.style.AxesChartStyler;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.None;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class AdaptiveBulkheadGraphTest {

    private static final boolean DRAW_GRAPHS = false;
    private static final int CALLS = 300;
    private static final Random NON_RANDOM = new Random(0);
    private static final int SLOW_CALL_DURATION_THRESHOLD = 200;
    private static final int RATE_THRESHOLD = 50;
    private static final int MAX_CONCURRENT_CALLS = 100;
    private static final int MIN_CONCURRENT_CALLS = 10;
    private static final int INITIAL_CONCURRENT_CALLS = 30;
    private AdaptiveBulkheadStateMachine bulkhead;
    private List<Double> time = new ArrayList<>();
    private List<Integer> concurrencyLimitData = new ArrayList<>();
    private List<Float> slowCallsRateData = new ArrayList<>();
    private List<Float> errorCallsRateData = new ArrayList<>();

    @Before
    public void setup() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .increaseSummand(5)
            .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
            .minConcurrentCalls(MIN_CONCURRENT_CALLS)
            .initialConcurrentCalls(INITIAL_CONCURRENT_CALLS)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(5)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .failureRateThreshold(RATE_THRESHOLD)
            .slowCallRateThreshold(RATE_THRESHOLD)
            .slowCallDurationThreshold(Duration.ofMillis(SLOW_CALL_DURATION_THRESHOLD))
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead.of("test", config);
        bulkhead.getEventPublisher().onSuccess(this::recordCallStats);
        bulkhead.getEventPublisher().onError(this::recordCallStats);
    }

    @Test
    public void testSlowCalls() {
        for (int i = 0; i < CALLS; i++) {
            bulkhead.onSuccess(nextLatency(), TimeUnit.MILLISECONDS);
        }
        drawGraph("testSlowCalls");
    }

    @Test
    public void testFailedCalls() {
        Throwable failure = new Throwable();

        for (int i = 0; i < CALLS; i++) {
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
        Throwable failure = new Throwable();

        for (int i = 0; i < CALLS; i++) {
            bulkhead.onError(1, TimeUnit.MILLISECONDS, failure);
        }
        drawGraph("testFailedCallsOnly");
    }

    @Test
    public void testSuccessfulCallsOnly() {
        for (int i = 0; i < CALLS; i++) {
            bulkhead.onSuccess(1, TimeUnit.MILLISECONDS);
        }
        drawGraph("testSuccessfulCallsOnly");
    }

    @Test
    public void testSlowCallsWithLowMinConcurrentCalls() {
        AdaptiveBulkheadConfig config = AdaptiveBulkheadConfig.custom()
            .increaseSummand(1)
            .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
            .minConcurrentCalls(10)
            .initialConcurrentCalls(20)
            .minimumNumberOfCalls(1)
            .slidingWindowType(AdaptiveBulkheadConfig.SlidingWindowType.TIME_BASED)
            .failureRateThreshold(RATE_THRESHOLD)
            .slowCallRateThreshold(RATE_THRESHOLD)
            .slowCallDurationThreshold(Duration.ofMillis(SLOW_CALL_DURATION_THRESHOLD))
            .build();
        bulkhead = (AdaptiveBulkheadStateMachine) AdaptiveBulkhead
            .of("test", config);
        bulkhead.getEventPublisher().onSuccess(this::recordCallStats);
        bulkhead.getEventPublisher().onError(this::recordCallStats);
        Throwable failure = new Throwable();

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 5; i++) {
                bulkhead.onSuccess(1, TimeUnit.MILLISECONDS);
            }
            for (int i = 0; i < 3; i++) {
                bulkhead.onError(System.currentTimeMillis(), TimeUnit.MILLISECONDS, failure);
            }
        }
        drawGraph("WithLowMinConcurrentCalls");
    }

    private int nextLatency() {
        return NON_RANDOM.nextInt((int) (1.8f * SLOW_CALL_DURATION_THRESHOLD));
    }

    private boolean nextErrorOccurred() {
        return NON_RANDOM.nextInt(2) == 0;
    }

    private void recordCallStats(AbstractAdaptiveBulkheadEvent event) {
        AdaptiveBulkheadMetrics metrics = (AdaptiveBulkheadMetrics) bulkhead.getMetrics();
        concurrencyLimitData.add(metrics.getMaxAllowedConcurrentCalls());
        time.add((double) concurrencyLimitData.size());
        slowCallsRateData.add(MAX_CONCURRENT_CALLS * metrics.getSnapshot().getSlowCallRate() / 100);
        errorCallsRateData.add(MAX_CONCURRENT_CALLS * metrics.getSnapshot().getFailureRate() / 100);
    }

    private void drawGraph(String testName) {
        if (!DRAW_GRAPHS) {
            return;
        }
        XYChart chart = new XYChartBuilder()
            .title(getClass().getSimpleName() + " - " + testName)
            .width(2200)
            .height(800)
            .xAxisTitle("time")
            .yAxisTitle("concurrency limit")
            .build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        //noinspection SuspiciousNameCombination
        chart.getStyler().setYAxisLabelAlignment(AxesChartStyler.TextAlignment.Right);
        chart.getStyler().setPlotMargin(0);
        chart.getStyler().setPlotContentSize(.95);
        drawAnnotationText(chart, "RATE_THRESHOLD",
            (int) bulkhead.getBulkheadConfig().getFailureRateThreshold());
        drawAnnotationText(chart, "MAX_CONCURRENT_CALLS",
            bulkhead.getBulkheadConfig().getMaxConcurrentCalls());
        drawAnnotationText(chart, "MIN_CONCURRENT_CALLS",
            bulkhead.getBulkheadConfig().getMinConcurrentCalls());
        drawAnnotationText(chart, "INITIAL_CONCURRENT_CALLS",
            bulkhead.getBulkheadConfig().getInitialConcurrentCalls());

        drawAreaSeries(chart, "slowCallsRate", slowCallsRateData, Color.LIGHT_GRAY);
        drawAreaSeries(chart, "failureCallsRate", errorCallsRateData, Color.GRAY);
        chart.addSeries("concurrency limit", time, concurrencyLimitData)
            .setMarkerColor(Color.CYAN)
            .setLineColor(Color.BLUE);
        try {
            BitmapEncoder.saveJPGWithQuality(chart, "./" + testName + ".jpg", 0.95f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawAnnotationText(XYChart chart, String text, int y) {
        chart.addAnnotation(new AnnotationText(text, 5, y, false));
    }

    private void drawAreaSeries(XYChart chart, String text, List<Float> yData, Color color) {
        chart.addSeries(text, time, yData)
            .setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area)
            .setMarker(new None())
            .setLineColor(color)
            .setFillColor(color);
    }
}