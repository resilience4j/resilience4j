/*
 *
 *  Copyright 2019: Bohdan Storozhuk, Mahmoud Romeh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead.adaptive.internal;

import java.util.Arrays;

/**
 * MovingAverageWindow based into moving average
 */
class MovingAverageWindow {
	private int cursor = 0;
	private final double[] window;

	public MovingAverageWindow(int size, long fillWith) {
		if (size < 2) {
			throw new IllegalArgumentException("window size should be bigger than 1");
		}
		window = new double[size];
		Arrays.fill(window, fillWith);
	}

	public boolean measure(double measurement) {
		window[cursor] = measurement;
		cursor = (cursor + 1) % window.length;
		return cursor == 0;
	}

	public double average() {
		double sum = 0;
		for (double sample : window) {
			sum += sample;
		}
		return sum / window.length;
	}

	public double standardDeviation() {
		double currentAverage = average();
		double accumulator = 0;
		for (double sample : window) {
			accumulator += (sample - currentAverage) * (sample - currentAverage);
		}
		if (window.length <= 50) {
			// use Bessel's correction to calculate sample standard deviation
			return (long) Math.sqrt((1.0d / (window.length - 1)) * accumulator);
		}
		return Math.sqrt(((accumulator)) / window.length);
	}

	@Override
	public String toString() {
		return "MeasurementWindow{" +
				"window=" + Arrays.toString(window) +
				'}';
	}
}
