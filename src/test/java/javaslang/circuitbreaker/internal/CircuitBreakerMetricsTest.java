/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker.internal;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerMetricsTest {


    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerMetricsTest.class);

    @Test
    public void testDefaultCircuitBreakerMetrics(){
        CircuitBreakerMetrics circuitBreakerMetrics = new CircuitBreakerMetrics(10);
        assertThat(circuitBreakerMetrics.getMaxNumberOfBufferedCalls()).isEqualTo(10);

        circuitBreakerMetrics.recordSuccess();
        circuitBreakerMetrics.recordSuccess();
        circuitBreakerMetrics.recordFailure();
        circuitBreakerMetrics.recordFailure();

        assertThat(circuitBreakerMetrics.getNumberOfBufferedCalls()).isEqualTo(4);
        assertThat(circuitBreakerMetrics.getNumberOfFailedCalls()).isEqualTo(2);

        // The failure rate must be -1, because the number of measured calls is below the buffer size of 10
        assertThat(circuitBreakerMetrics.getFailureRate()).isEqualTo(-1);

        circuitBreakerMetrics.recordFailure();
        circuitBreakerMetrics.recordFailure();
        circuitBreakerMetrics.recordFailure();
        circuitBreakerMetrics.recordFailure();
        circuitBreakerMetrics.recordSuccess();
        circuitBreakerMetrics.recordSuccess();
        circuitBreakerMetrics.recordSuccess();
        circuitBreakerMetrics.recordSuccess();

        // 12 calls have been recorded, but only 10 are stored in the RingBitSet. 4 successes and 6 failures.
        // The failure rate must be 60%, because the number of measured calls is above the minimum number of measured calls.
        assertThat(circuitBreakerMetrics.getNumberOfBufferedCalls()).isEqualTo(10);
        assertThat(circuitBreakerMetrics.getNumberOfFailedCalls()).isEqualTo(6);
        assertThat(circuitBreakerMetrics.getFailureRate()).isEqualTo(60);
    }
}
