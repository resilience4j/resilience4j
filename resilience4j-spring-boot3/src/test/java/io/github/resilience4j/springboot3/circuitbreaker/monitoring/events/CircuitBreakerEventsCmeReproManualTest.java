/*
 * Copyright 2020 Vijay Ram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.springboot3.circuitbreaker.monitoring.events;

import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.springboot3.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpoint;
import io.github.resilience4j.springboot3.service.test.CbOnlyReproService;
import io.github.resilience4j.springboot3.service.test.TestApplication;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class,
    properties = "resilience4j.circuitbreaker.backends.backendA.eventConsumerBufferSize=4096")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Ignore("Manual/flaky CME reproduction test")
public class CircuitBreakerEventsCmeReproManualTest {

    @Autowired
    private CircuitBreakerEventsEndpoint circuitBreakerEventsEndpoint;

    @Autowired
    private CbOnlyReproService cbOnlyReproService;

    @Test
    public void reproduceConcurrentModificationExceptionOnCircuitBreakerEventsEndpoint()
        throws InterruptedException {
        int durationSec = Integer.getInteger("cme.repro.durationSec", 150);
        int workersWork = Integer.getInteger("cme.repro.workersWork", 70);
        int workersEvents = Integer.getInteger("cme.repro.workersEvents", 24);
        int attempts = Integer.getInteger("cme.repro.attempts", 1);
        int pauseBetweenAttemptsMs = Integer.getInteger("cme.repro.pauseBetweenAttemptsMs", 0);
        List<String> attemptSummaries = new ArrayList<>();
        int totalCmeCount = 0;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            AttemptStats stats = runReproductionAttempt(durationSec, workersWork, workersEvents);
            totalCmeCount += stats.cmeCount.get();
            int minEventsSeen = stats.minEventsSeen.get() == Integer.MAX_VALUE
                ? -1
                : stats.minEventsSeen.get();
            attemptSummaries.add(String.format(
                "attempt%s: cme=%s, fail=%s, workCalls=%s, endpointCalls=%s, maxEventsSeen=%s, minEventsSeen=%s, sampledSizes=%s, workloadExceptions=%s, endpointFailures=%s",
                attempt,
                stats.cmeCount.get(),
                stats.failureCount.get(),
                stats.workCalls.get(),
                stats.endpointCalls.get(),
                stats.maxEventsSeen.get(),
                minEventsSeen,
                stats.sampledSizes,
                stats.workloadExceptionCounts,
                stats.failureSamples));
            if (stats.cmeCount.get() > 0) {
                return;
            }
            if (attempt < attempts && pauseBetweenAttemptsMs > 0) {
                TimeUnit.MILLISECONDS.sleep(pauseBetweenAttemptsMs);
            }
        }

        assertThat(totalCmeCount)
            .withFailMessage(
                "CME was not observed. durationSec=%s, workersWork=%s, workersEvents=%s, attempts=%s, summaries=%s. Repro command: -Dcme.repro.durationSec=150 -Dcme.repro.workersWork=70 -Dcme.repro.workersEvents=24 -Dcme.repro.attempts=1. For stronger retries use -Dcme.repro.attempts=3.",
                durationSec,
                workersWork,
                workersEvents,
                attempts,
                attemptSummaries)
            .isGreaterThan(0);
    }

    private ExecutorService newFixedPool(String threadPrefix, int workers) {
        AtomicInteger threadCounter = new AtomicInteger();
        return Executors.newFixedThreadPool(workers, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(threadPrefix + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    private void awaitTermination(String executorName, ExecutorService executorService)
        throws InterruptedException {
        boolean terminated = executorService.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(terminated)
            .withFailMessage("%s did not terminate within timeout", executorName)
            .isTrue();
    }

    private void runWorkLoad(
        CountDownLatch startGate,
        AtomicBoolean stop,
        AtomicInteger workCalls,
        Map<String, AtomicInteger> workloadExceptionCounts) {
        awaitStart(startGate);
        while (!stop.get() && !Thread.currentThread().isInterrupted()) {
            boolean failCall = ThreadLocalRandom.current().nextBoolean();
            try {
                cbOnlyReproService.doSomething(failCall);
            } catch (IOException ignored) {
            } catch (RuntimeException runtimeException) {
                incrementExceptionCount(workloadExceptionCounts, runtimeException);
            }
            workCalls.incrementAndGet();
        }
    }

    private void runEndpointLoad(
        CountDownLatch startGate,
        AtomicBoolean stop,
        AtomicInteger endpointCalls,
        AtomicInteger failureCount,
        AtomicInteger cmeCount,
        ConcurrentLinkedQueue<String> failureSamples,
        AtomicInteger maxEventsSeen,
        AtomicInteger minEventsSeen,
        ConcurrentLinkedQueue<Integer> sampledSizes) {
        awaitStart(startGate);
        while (!stop.get() && !Thread.currentThread().isInterrupted()) {
            try {
                CircuitBreakerEventsEndpointResponse response =
                    circuitBreakerEventsEndpoint.getAllCircuitBreakerEvents();
                endpointCalls.incrementAndGet();
                int eventCount = getEventCount(response);
                updateMax(maxEventsSeen, eventCount);
                updateMin(minEventsSeen, eventCount);
                if (sampledSizes.size() < 10 && ThreadLocalRandom.current().nextInt(1000) == 0) {
                    sampledSizes.add(eventCount);
                }
            } catch (RuntimeException exception) {
                recordFailure(exception, failureCount, cmeCount, failureSamples);
            }
        }
    }

    private AttemptStats runReproductionAttempt(int durationSec, int workersWork, int workersEvents)
        throws InterruptedException {
        AttemptStats stats = new AttemptStats();
        AtomicBoolean stop = new AtomicBoolean(false);
        CountDownLatch startGate = new CountDownLatch(1);

        ExecutorService workExecutor = newFixedPool("cb-cme-work-", workersWork);
        ExecutorService eventsExecutor = newFixedPool("cb-cme-events-", workersEvents);
        for (int i = 0; i < workersWork; i++) {
            workExecutor.submit(() -> runWorkLoad(
                startGate,
                stop,
                stats.workCalls,
                stats.workloadExceptionCounts));
        }
        for (int i = 0; i < workersEvents; i++) {
            eventsExecutor.submit(() -> runEndpointLoad(
                startGate,
                stop,
                stats.endpointCalls,
                stats.failureCount,
                stats.cmeCount,
                stats.failureSamples,
                stats.maxEventsSeen,
                stats.minEventsSeen,
                stats.sampledSizes));
        }

        startGate.countDown();
        TimeUnit.SECONDS.sleep(durationSec);
        stop.set(true);

        workExecutor.shutdownNow();
        eventsExecutor.shutdownNow();
        awaitTermination("workExecutor", workExecutor);
        awaitTermination("eventsExecutor", eventsExecutor);
        return stats;
    }

    private void recordFailure(
        Throwable throwable,
        AtomicInteger failureCount,
        AtomicInteger cmeCount,
        ConcurrentLinkedQueue<String> failureSamples) {
        failureCount.incrementAndGet();
        if (containsConcurrentModificationException(throwable)) {
            cmeCount.incrementAndGet();
        }
        if (failureSamples.size() < 3) {
            failureSamples.add(stackTraceSnippet(throwable));
        }
    }

    private boolean containsConcurrentModificationException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.util.ConcurrentModificationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private void incrementExceptionCount(
        Map<String, AtomicInteger> exceptionCounts,
        RuntimeException runtimeException) {
        String key = runtimeException.getClass().getSimpleName();
        exceptionCounts.computeIfAbsent(key, unused -> new AtomicInteger()).incrementAndGet();
    }

    private int getEventCount(CircuitBreakerEventsEndpointResponse response) {
        if (response == null || response.getCircuitBreakerEvents() == null) {
            return 0;
        }
        return response.getCircuitBreakerEvents().size();
    }

    private void updateMax(AtomicInteger currentMax, int newValue) {
        currentMax.accumulateAndGet(newValue, Math::max);
    }

    private void updateMin(AtomicInteger currentMin, int newValue) {
        currentMin.accumulateAndGet(newValue, Math::min);
    }

    private String stackTraceSnippet(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return trim(stringWriter.toString(), 1200);
    }

    private void awaitStart(CountDownLatch startGate) {
        try {
            startGate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class AttemptStats {
        private final AtomicInteger workCalls = new AtomicInteger();
        private final AtomicInteger endpointCalls = new AtomicInteger();
        private final AtomicInteger failureCount = new AtomicInteger();
        private final AtomicInteger cmeCount = new AtomicInteger();
        private final AtomicInteger maxEventsSeen = new AtomicInteger(-1);
        private final AtomicInteger minEventsSeen = new AtomicInteger(Integer.MAX_VALUE);
        private final ConcurrentHashMap<String, AtomicInteger> workloadExceptionCounts =
            new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<Integer> sampledSizes = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> failureSamples = new ConcurrentLinkedQueue<>();
    }
}
