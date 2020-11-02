/*
 *
 *  Copyright 2020 krnsaurabh
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

import io.github.resilience4j.test.TestContextPropagators;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

public class ContextAwareScheduledThreadPoolTest {

    private ContextAwareScheduledThreadPool schedulerService;

    @Before
    public void initialise() {
        schedulerService = new ContextAwareScheduledThreadPool(
            20,
            Stream.of(new TestContextPropagators.TestThreadLocalContextPropagatorWithHolder())
                .collect(Collectors.toList()));
    }

    @Test
    public void testConfigs() {
        assertThat(schedulerService.getCorePoolSize()).isEqualTo(20);
    }

    @Test
    public void testScheduleRunnablePropagatesContext() throws Exception{
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> {
            TestThreadLocalContextHolder.get().orElseThrow(() -> new RuntimeException("Found No Context"));
        }, 0, TimeUnit.MILLISECONDS);
        schedule.get();
    }

    @Test
    public void testScheduleCallablePropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> TestThreadLocalContextHolder.get().orElse(null), 0, TimeUnit.MILLISECONDS);
        waitAtMost(1, TimeUnit.SECONDS).until(matches(() ->
            assertThat(schedule.get()).isEqualTo("ValueShouldCrossThreadBoundary")));
    }

    @Test
    public void testScheduleCallableWithDelayPropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> TestThreadLocalContextHolder.get().orElse(null), 100, TimeUnit.MILLISECONDS);
        waitAtMost(200, TimeUnit.MILLISECONDS).until(matches(() ->
            assertThat(schedule.get()).isEqualTo("ValueShouldCrossThreadBoundary")));
    }

    @Test
    public void testMDC() {
        MDC.put("key", "value");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final ScheduledFuture<Map<String, String>> scheduledFuture = this.schedulerService
            .schedule(TestThreadLocalContextHolder::getMDCContext, 0, TimeUnit.MILLISECONDS);

        waitAtMost(1, TimeUnit.SECONDS).until(matches(() ->
            assertThat(scheduledFuture.get()).hasSize(2).containsExactlyEntriesOf(contextMap)));
    }

    @Test
    public void testMDCWithoutContextPropagator() {
        final ContextAwareScheduledThreadPool schedulerService = new ContextAwareScheduledThreadPool(
            10);

        MDC.put("key", "value");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final ScheduledFuture<Map<String, String>> scheduledFuture = schedulerService
            .schedule(TestThreadLocalContextHolder::getMDCContext, 0, TimeUnit.MILLISECONDS);

        waitAtMost(1, TimeUnit.SECONDS).until(matches(() ->
            assertThat(scheduledFuture.get()).hasSize(2).containsExactlyEntriesOf(contextMap)));
    }
}
