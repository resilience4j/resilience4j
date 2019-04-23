/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.core;

import com.statemachinesystems.mockclock.MockClock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

public class StopWatchTest {

    private static final Logger LOG = LoggerFactory.getLogger(StopWatchTest.class);

    @Test
    public void testStopWatch() {
        MockClock mockClock = MockClock.at(2019, 1, 1, 12, 0, 0, ZoneId.of("UTC"));

        StopWatch watch = new StopWatch(mockClock);

        mockClock.advanceBy(Duration.ofSeconds(5));

        Duration duration = watch.stop();
        LOG.info(watch.toString());
        assertThat(duration.getSeconds()).isEqualTo(5);
    }
}
