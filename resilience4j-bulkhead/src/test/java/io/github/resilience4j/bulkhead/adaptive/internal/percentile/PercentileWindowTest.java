package io.github.resilience4j.bulkhead.adaptive.internal.percentile;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

/**
 * unit test moving window
 */
public class PercentileWindowTest {


	@Test(expected = IllegalArgumentException.class)
	public void testPreconditionOfTheWindowSize() {
		PercentileWindow percentileWindow = new PercentileWindow(0, 1);
	}

	@Test
	public void testToString() {
		PercentileWindow percentileWindow = new PercentileWindow(2, 1);
		assertThat(percentileWindow.toString()).isEqualTo("PercentileWindow{window=[1.0, 1.0]}");

	}

}