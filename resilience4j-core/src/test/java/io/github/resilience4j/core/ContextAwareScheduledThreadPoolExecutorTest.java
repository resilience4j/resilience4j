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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import io.github.resilience4j.core.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;

class ContextAwareScheduledThreadPoolExecutorTest {

    private ContextAwareScheduledThreadPoolExecutor schedulerService;

    @BeforeEach
    void initialise() {
        schedulerService = ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
            .corePoolSize(20)
            .contextPropagators(new TestContextPropagators.TestThreadLocalContextPropagatorWithHolder())
            .build();
    }

    @Test
    void configs() {
        assertThat(schedulerService.getCorePoolSize()).isEqualTo(20);
        assertThat(schedulerService.getThreadFactory()).isInstanceOf(NamingThreadFactory.class);
        assertThat(schedulerService.getContextPropagators()).hasSize(1)
            .hasOnlyElementsOfTypes(TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.class);
    }

    @Test
    void throwsExceptionWhenCorePoolSizeLessThanOne() {
        assertThatThrownBy(() -> ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(0)
            .build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scheduleRunnablePropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> {
            TestThreadLocalContextHolder.get().orElseThrow(() -> new RuntimeException("Found No Context"));
        }, 0, TimeUnit.MILLISECONDS);
        assertThatCode(() -> schedule.get()).doesNotThrowAnyException();
    }

    @Test
    void scheduleRunnableWithDelayPropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> {
            TestThreadLocalContextHolder.get().orElseThrow(() -> new RuntimeException("Found No Context"));
        }, 100, TimeUnit.MILLISECONDS);
        try{
            await().atMost(200, TimeUnit.MILLISECONDS).untilAsserted(() -> schedule.get());
        } catch (Exception exception) {
            fail("Must not throw an exception");
        }
        
    }

    @Test
    void threadFactory() {
        final ScheduledFuture<String> schedule = schedulerService.schedule(() -> Thread.currentThread().getName(), 0, TimeUnit.MILLISECONDS);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(schedule.get()).contains("ContextAwareScheduledThreadPool"));
    }

    @Test
    void scheduleCallablePropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> TestThreadLocalContextHolder.get().orElse(null), 0, TimeUnit.MILLISECONDS);
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(schedule.get()).isEqualTo("ValueShouldCrossThreadBoundary"));
    }

    @Test
    void scheduleCallableWithDelayPropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final ScheduledFuture<?> schedule = schedulerService.schedule(() -> TestThreadLocalContextHolder.get().orElse(null), 100, TimeUnit.MILLISECONDS);
        await().atMost(200, TimeUnit.MILLISECONDS).untilAsserted(() ->
            assertThat(schedule.get()).isEqualTo("ValueShouldCrossThreadBoundary"));
    }

    @Test
    void completableFuturePropagatesContext() {
        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            assertThat(Thread.currentThread().getName()).contains("ContextAwareScheduledThreadPool");
            return (String) TestThreadLocalContextHolder.get().orElse(null);
        }, schedulerService);
        await().atMost(200, TimeUnit.MILLISECONDS).untilAsserted(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary"));
    }

    @Test
    void completableFuturePropagatesMDCContext() {
        MDC.put("key", "ValueShouldCrossThreadBoundary");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            assertThat(Thread.currentThread().getName()).isEqualTo("ContextAwareScheduledThreadPool-1");
            assertThat(MDC.getCopyOfContextMap()).containsExactlyEntriesOf(contextMap);
            return MDC.getCopyOfContextMap().get("key");
        }, schedulerService);
        await().atMost(200, TimeUnit.MILLISECONDS).untilAsserted(() ->
            assertThat(completableFuture).isCompletedWithValue("ValueShouldCrossThreadBoundary"));
    }

    @Test
    void scheduleCallablePropagatesMDCContext() {
        MDC.put("key", "value");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final ScheduledFuture<Map<String, String>> scheduledFuture = this.schedulerService
            .schedule(MDC::getCopyOfContextMap, 0, TimeUnit.MILLISECONDS);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(scheduledFuture.get()).containsExactlyEntriesOf(contextMap));
    }

    @Test
    void mdcWithoutContextPropagator() {
        final ContextAwareScheduledThreadPoolExecutor schedulerService = ContextAwareScheduledThreadPoolExecutor
            .newScheduledThreadPool()
            .corePoolSize(10)
            .build();

        MDC.put("key", "value");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final ScheduledFuture<Map<String, String>> scheduledFuture = schedulerService
            .schedule(MDC::getCopyOfContextMap, 0, TimeUnit.MILLISECONDS);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(scheduledFuture.get()).containsExactlyEntriesOf(contextMap));
    }
}
