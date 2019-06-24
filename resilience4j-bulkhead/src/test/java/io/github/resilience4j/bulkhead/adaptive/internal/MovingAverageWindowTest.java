package io.github.resilience4j.bulkhead.adaptive.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

/**
 * unit test moving window
 */
public class MovingAverageWindowTest {


	@Test(expected = IllegalArgumentException.class)
	public void testPreconditionOfTheWindowSize() {
		MovingAverageWindow movingAverageWindow = new MovingAverageWindow(0, 1);
	}

	@Test
	public void testToString() {
		MovingAverageWindow movingAverageWindow = new MovingAverageWindow(2, 1);
		assertThat(movingAverageWindow.toString()).isEqualTo("MeasurementWindow{window=[1.0, 1.0]}");

	}

}