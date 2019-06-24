package io.github.resilience4j.bulkhead.adaptive.internal;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import io.github.resilience4j.adapter.RxJava2Adapter;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.reactivex.subscribers.TestSubscriber;

/**
 * test the adoptive bulkhead limiter logic
 */
public class AdaptiveBulkheadWithLimiterTest {
	private AdaptiveBulkhead bulkhead;
	private AdaptiveBulkheadConfig config;
	private TestSubscriber<BulkheadLimit.Type> testSubscriber;
	// enable if u need to see the graphs of the executions
	private boolean drawGraphs = false;

	private List<Double> averageLatency = new ArrayList<>();
	private List<Double> currentMaxLatency = new ArrayList<>();

	@Before
	public void setup() {
		config = AdaptiveBulkheadConfig.builder()
				.maxAcceptableRequestLatency(1.5)
				.desirableAverageThroughput(2)
				.desirableOperationLatency(1)
				.build();

		bulkhead = AdaptiveBulkhead.of("test", config);
		testSubscriber = RxJava2Adapter.toFlowable(bulkhead.getEventPublisher())
				.map(BulkheadLimit::getEventType)
				.test();
	}

	@Test
	public void testLimiter() {
		List<Double> time = new ArrayList<>();
		List<Double> desiredLatency = new ArrayList<>();
		List<Double> acceptableLatency = new ArrayList<>();
		List<Double> maxConcurrentCalls = new ArrayList<>();
		AtomicInteger count = new AtomicInteger();
		if (drawGraphs) {
			bulkhead.getEventPublisher().onEvent(bulkhead -> {
				averageLatency.add(Double.valueOf(bulkhead.eventData().get("averageLatencySeconds")));
				currentMaxLatency.add(Double.valueOf(bulkhead.eventData().get("currentMaxLatency")));
				maxConcurrentCalls.add(Double.valueOf(bulkhead.eventData().get("newMaxConcurrentCalls")));
				time.add((double) count.incrementAndGet() + 1);
				desiredLatency.add(config.getDesirableOperationLatency());
				acceptableLatency.add(config.getMaxAcceptableRequestLatency());
			});
		}
		// if u like to get the graphs , increase the number of iterations to have better distribution
		for (int i = 0; i < 2000; i++) {
			bulkhead.onComplete(Duration.ofMillis(randomLatency(200, 2100).get()));

		}
		// number of adoption concurrency iterations based into window size which will be every 100 measurements
		testSubscriber.assertValueCount(20);

		if (drawGraphs) {
			// Create Chart
			XYChart chart = new XYChartBuilder().width(800).height(600).title(getClass().getSimpleName()).xAxisTitle("time").yAxisTitle("latency").build();
			chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
			chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
			chart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
			chart.getStyler().setYAxisDecimalPattern("Seconds #,###.##");
			chart.getStyler().setPlotMargin(0);
			chart.getStyler().setPlotContentSize(.95);


			chart.addSeries("averageLatency", time, averageLatency);
			chart.addSeries("currentMaxLatency", time, currentMaxLatency);
			chart.addSeries("desiredLatency", time, desiredLatency);
			chart.addSeries("acceptableLatency", time, acceptableLatency);
			try {
				BitmapEncoder.saveJPGWithQuality(chart, "./AdaptiveBulkhead.jpg", 0.95f);
			} catch (IOException e) {
				e.printStackTrace();
			}


			// Create Chart
			XYChart chart2 = new XYChartBuilder().width(800).height(600).title(getClass().getSimpleName()).xAxisTitle("time").yAxisTitle("Concurrency limit").build();
			chart2.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
			chart2.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
			chart2.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
			chart2.getStyler().setYAxisDecimalPattern("ConcurrentCalls #");
			chart2.getStyler().setPlotMargin(0);
			chart2.getStyler().setPlotContentSize(.95);


			chart2.addSeries("MaxConcurrentCalls", time, maxConcurrentCalls);
			try {
				BitmapEncoder.saveJPGWithQuality(chart2, "./AdaptiveBulkheadConcurrency.jpg", 0.95f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public Supplier<Long> randomLatency(int min, int max) {
		return () -> min + ThreadLocalRandom.current().nextLong(max - min);
	}
}