package io.github.resilience4j.bulkhead.adaptive.internal.amid;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AIMDConfig;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;

/**
 * test the adoptive bulkhead limiter logic
 */
public class AimdLimiterTest {
	private Bulkhead bulkhead;
	private AdaptiveBulkheadConfig<AIMDConfig> config;
	private AIMDLimiter aimdLimiter;
	// enable if u need to see the graphs of the executions
	private boolean drawGraphs = false;

	@Before
	public void setup() {
		config = AdaptiveBulkheadConfig.<AIMDConfig>builder().config(AIMDConfig.builder().maxConcurrentRequestsLimit(50)
				.minConcurrentRequestsLimit(5)
				.slidingWindowSize(5)
				.slidingWindowTime(2)
				.failureRateThreshold(50)
				.slowCallRateThreshold(50)
				.slowCallDurationThreshold(200)
				.build()).build();

		BulkheadConfig currentConfig = BulkheadConfig.custom()
				.maxConcurrentCalls(5)
				.maxWaitDuration(Duration.ofMillis(0))
				.build();

		bulkhead = new SemaphoreBulkhead("test-internal", currentConfig);

		aimdLimiter = new AIMDLimiter(config);

	}

	@Test
	public void testLimiter() throws InterruptedException {
		List<Double> time = new ArrayList<>();
		List<Double> maxConcurrentCalls = new ArrayList<>();
		AtomicInteger inFlightCounter = new AtomicInteger();
		AtomicInteger count = new AtomicInteger();
		ExecutorService executorService = Executors.newFixedThreadPool(6);
		// if u like to get the graphs , increase the number of iterations to have better distribution
		Collection<Callable<String>> threads = new ArrayList<>(2000);
		for (int i = 0; i < 3000; i++) {
			final Duration duration = Duration.ofMillis(randomLatency(5, 400));
			final LimitResult limitResult = aimdLimiter.adaptLimitIfAny(duration.toMillis(), true, randomLInFlight(1, 50));
			if (drawGraphs) {
				maxConcurrentCalls.add((double) limitResult.getLimit());
				time.add((double) count.incrementAndGet());
			}
		}


		if (drawGraphs) {
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

	public long randomLatency(int min, int max) {
		return min + ThreadLocalRandom.current().nextLong(max - min);
	}

	public int randomLInFlight(int min, int max) {
		return min + ThreadLocalRandom.current().nextInt(max - min);
	}
}