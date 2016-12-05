/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.robwin.metrics;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class StopWatchTest {

    private static final Logger LOG = LoggerFactory.getLogger(StopWatchTest.class);

    @Test
    public void testStopWatch() throws InterruptedException {
        StopWatch watch = StopWatch.start("id");
        Thread.sleep(100);
        Duration duration = watch.stop().getElapsedDuration();
        LOG.info(watch.toString());
        Assertions.assertThat(duration.toMillis()).isGreaterThanOrEqualTo(100).isLessThan(110);
    }
}
