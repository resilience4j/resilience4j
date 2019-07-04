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
package io.github.resilience4j.bulkhead.adaptive.internal.percentile;

import java.util.Arrays;

/**
 * window limit percentile
 */
class PercentileWindow {
	private int cursor = 0;
	private final double[] window;

	public PercentileWindow(int size, long fillWith) {
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

	/**
	 * @param percent percentile of data desired
	 * @return data at the asked-for percentile.  Interpolation is used if exactness is not possible
	 * @see <a href="http://en.wikipedia.org/wiki/Percentile">Percentile (Wikipedia)</a>
	 * @see <a href="http://cnx.org/content/m10805/latest/">Percentile</a>
	 */
	public double computePercentile(double percent) {
		// Some just-in-case edge cases
		Arrays.sort(window);
		int length = window.length;
		if (length <= 0) {
			return 0;
		} else if (percent <= 0.0) {
			return window[0];
		} else if (percent >= 100.0) {
			return window[length - 1];
		}

		// ranking (http://en.wikipedia.org/wiki/Percentile#Alternative_methods)
		double rank = (percent / 100.0) * length;

		// linear interpolation between closest ranks
		int iLow = (int) Math.floor(rank);
		int iHigh = (int) Math.ceil(rank);
		assert 0 <= iLow && iLow <= rank && rank <= iHigh && iHigh <= length;
		assert (iHigh - iLow) <= 1;
		if (iHigh >= length) {
			// Another edge case
			return window[length - 1];
		} else if (iLow == iHigh) {
			return window[iLow];
		} else {
			// Interpolate between the two bounding values
			return (int) (window[iLow] + (rank - iLow) * (window[iHigh] - window[iLow]));
		}
	}


	@Override
	public String toString() {
		return "PercentileWindow{" +
				"window=" + Arrays.toString(window) +
				'}';
	}
}
