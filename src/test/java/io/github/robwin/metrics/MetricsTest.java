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
package io.github.robwin.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import javaslang.collection.Stream;
import javaslang.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.BDDAssertions.assertThat;

public class MetricsTest {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsTest.class);

    private Timer timer;

    @Before
    public void setUp(){
        MetricRegistry metricRegistry = new MetricRegistry();
        timer = metricRegistry.timer(name("test"));
    }

    @Test
    public void shouldMeasureTime() throws Throwable {
        // When I create a long running supplier
        Try.CheckedSupplier<String> supplier = () -> {
            Thread.sleep(2000);
            return "Hello world";
        };

        // And measure the time with Metrics
        Try.CheckedSupplier<String> timedSupplier = Metrics.timedCheckedSupplier(supplier, timer);

        String value = timedSupplier.get();

        // Then the counter of metrics should be one and the
        assertThat(timer.getCount()).isEqualTo(1);
        // and the mean time should be greater than 2
        assertThat(timer.getSnapshot().getMean()).isGreaterThan(2);

        assertThat(value).isEqualTo("Hello world");
    }

    @Test
    public void shouldMeasureInvocationRate() throws Throwable {
        // When I create a long running supplier
        Supplier<String> supplier = () -> "Hello world";

        // And measure the time with Metrics
        Supplier<String> timedSupplier = Metrics.timedSupplier(supplier, timer);

        Stream.range(0,10).forEach((i) -> timedSupplier.get());

        // Then the counter of metrics should be one and the
        assertThat(timer.getCount()).isEqualTo(10);

        Thread.sleep(15000);

        LOG.info("rate1: " + timer.getOneMinuteRate());
        LOG.info("rate5: " + timer.getFiveMinuteRate());
        LOG.info("rate15: " + timer.getFifteenMinuteRate());

    }
}
