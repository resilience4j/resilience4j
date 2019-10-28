package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.bulkhead.adaptive.internal.amid.AimdLimiter;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AimdConfig;
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
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * test the adoptive bulkhead limiter logic
 */
public class AimdLimiterTest {
	private AdaptiveBulkhead bulkhead;
	private AdaptiveBulkheadConfig<AimdConfig> config;
	private AimdLimiter aimdLimiter;
	// enable if u need to see the graphs of the executions
	private boolean drawGraphs = true;

	@Before
	public void setup() {
		config = AdaptiveBulkheadConfig.<AimdConfig>builder().config(AimdConfig.builder().maxConcurrentRequestsLimit(50)
				.minConcurrentRequestsLimit(5)
				.slidingWindowSize(5)
				.slidingWindowTime(2)
				.failureRateThreshold(50)
				.slowCallRateThreshold(50)
				.slowCallDurationThreshold(200)
				.build()).build();


		bulkhead = AdaptiveBulkhead.of("test", config);

		aimdLimiter = new AimdLimiter(config);

	}

	@Test
	public void testLimiter() throws InterruptedException {
		List<Double> time = new ArrayList<>();
		List<Double> maxConcurrentCalls = new ArrayList<>();
		AtomicInteger count = new AtomicInteger();
		// if u like to get the graphs , increase the number of iterations to have better distribution
		for (int i = 0; i < 3000; i++) {
			final Duration duration = Duration.ofMillis(randomLatency(5, 400));
			AdaptiveLimitBulkhead adaptiveLimitBulkhead = (AdaptiveLimitBulkhead) bulkhead;
			Random random = new Random();
			final LimitResult limitResult = adaptiveLimitBulkhead.record(duration.toMillis(), random.nextBoolean(), randomLInFlight(1, 50));
			if (drawGraphs) {
				maxConcurrentCalls.add((double) limitResult.getLimit());
				time.add((double) count.incrementAndGet());
			}
		}

		assertThat(config).isNotNull();
		assertThat(bulkhead).isNotNull();

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