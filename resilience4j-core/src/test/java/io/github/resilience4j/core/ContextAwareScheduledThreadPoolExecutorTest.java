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

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import io.github.resilience4j.core.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;

public class ContextAwareScheduledThreadPoolExecutorTest {

    private ContextAwareScheduledThreadPoolExecutor schedulerService;

    @Before
    public void initialise() {
        schedulerService = ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
            .corePoolSize(20)
            .contextPropagators(new TestContextPropagators.TestThreadLocalContextPropagatorWithHolder())
            .build();
    }

    @Test
    public void testConfigs() {
        assertThat(schedulerService.getCorePoolSize()).isEqualTo(20);
        assertThat(schedulerService.getThreadFactory()).isInstanceOf(NamingThreadFactory.class);
        assertThat(schedulerService.getContextPropagators()).hasSize(1)
            .hasOnlyElementsOfTypes(TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.class);
    }

    @Test
    public void throwsExceptionWhenCorePoolSizeLessThanOne() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->  ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(0)
            .build());
    }

    @Test
    public void testScheduleRunnablePropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> {
            TestThreadLocalContextHolder.get().orElseThrow(() -> new RuntimeException("Found No Context"));
        }, 0, TimeUnit.MILLISECONDS);
        assertThatCode(() -> schedule.get()).doesNotThrowAnyException();
    }

    @Test
    public void testScheduleRunnableWithDelayPropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> {
            TestThreadLocalContextHolder.get().orElseThrow(() -> new RuntimeException("Found No Context"));
        }, 100, TimeUnit.MILLISECONDS);
        try{
            await().atMost(200, TimeUnit.MILLISECONDS).until(matches(() -> schedule.get()));
        } catch (Exception exception) {
            Assertions.fail("Must not throw an exception");
        }
        
    }

    @Test
    public void testThreadFactory() {
        final ScheduledFuture<String> schedule = schedulerService.schedule(() -> Thread.currentThread().getName(), 0, TimeUnit.MILLISECONDS);
        waitAtMost(1, TimeUnit.SECONDS).until(matches(() ->
            assertThat(schedule.get()).contains("ContextAwareScheduledThreadPool")));
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
    public void testCompletableFuturePropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            assertThat(Thread.currentThread().getName()).contains("ContextAwareScheduledThreadPool");
            return (String) TestThreadLocalContextHolder.get().orElse(null);
        }, schedulerService);
        waitAtMost(200, TimeUnit.MILLISECONDS).until(matches(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary")));
    }

    @Test
    public void testCompletableFuturePropagatesMDCContext() {
        MDC.put("key", "ValueShouldCrossThreadBoundary");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            assertThat(Thread.currentThread().getName()).isEqualTo("ContextAwareScheduledThreadPool-1");
            assertThat(MDC.getCopyOfContextMap()).hasSize(2).containsExactlyEntriesOf(contextMap);
            return MDC.getCopyOfContextMap().get("key");
        }, schedulerService);
        waitAtMost(200, TimeUnit.MILLISECONDS).until(matches(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary")));
    }

    @Test
    public void testScheduleCallablePropagatesMDCContext() {
        MDC.put("key", "value");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final ScheduledFuture<Map<String, String>> scheduledFuture = this.schedulerService
            .schedule(MDC::getCopyOfContextMap, 0, TimeUnit.MILLISECONDS);

        waitAtMost(1, TimeUnit.SECONDS).until(matches(() ->
            assertThat(scheduledFuture.get()).hasSize(2).containsExactlyEntriesOf(contextMap)));
    }

    @Test
    public void testMDCWithoutContextPropagator() {
        final ContextAwareScheduledThreadPoolExecutor schedulerService = ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(10)
            .build();

        MDC.put("key", "value");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final ScheduledFuture<Map<String, String>> scheduledFuture = schedulerService
            .schedule(MDC::getCopyOfContextMap, 0, TimeUnit.MILLISECONDS);

        waitAtMost(1, TimeUnit.SECONDS).until(matches(() ->
            assertThat(scheduledFuture.get()).hasSize(2).containsExactlyEntriesOf(contextMap)));
    }
}
