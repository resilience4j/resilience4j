/*
 * Copyright 2026
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
package io.github.resilience4j.spring6.timelimiter.configure;

import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link TimeLimiterAspect}'s fallback scheduler honors the
 * {@code resilience4j.thread.type} system property when no
 * {@code ContextAwareScheduledThreadPoolExecutor} bean is provided.
 */
@RunWith(Parameterized.class)
public class TimeLimiterAspectVirtualThreadTest {

    private static final String SYS_PROP_KEY = "resilience4j.thread.type";
    private static final String EXPECTED_POOL_NAME = "TimeLimiterAspect";

    private final ThreadType threadType;
    private String originalProperty;
    private TimeLimiterAspect aspect;

    public TimeLimiterAspectVirtualThreadTest(ThreadType threadType) {
        this.threadType = threadType;
    }

    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> threadModes() {
        return Arrays.asList(new Object[][]{
            {ThreadType.PLATFORM},
            {ThreadType.VIRTUAL}
        });
    }

    @Before
    public void setUp() {
        originalProperty = System.getProperty(SYS_PROP_KEY);
        if (threadType == ThreadType.VIRTUAL) {
            System.setProperty(SYS_PROP_KEY, threadType.toString());
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
        // Pass null for contextAwareScheduledThreadPoolExecutor to force the
        // ExecutorServiceFactory fallback path under test.
        aspect = new TimeLimiterAspect(null, null, null, null, null, null);
    }

    @After
    public void tearDown() throws Exception {
        if (aspect != null) {
            aspect.close();
        }
        if (originalProperty != null) {
            System.setProperty(SYS_PROP_KEY, originalProperty);
        } else {
            System.clearProperty(SYS_PROP_KEY);
        }
    }

    @Test
    public void fallbackSchedulerShouldUseConfiguredThreadType() throws Exception {
        ScheduledExecutorService executor = extractExecutorService(aspect);

        boolean ranOnVirtual = executor
            .submit(() -> Thread.currentThread().isVirtual())
            .get(5, TimeUnit.SECONDS);

        assertThat(ranOnVirtual)
            .as("TimeLimiterAspect fallback scheduler must respect resilience4j.thread.type=%s",
                threadType)
            .isEqualTo(threadType == ThreadType.VIRTUAL);
    }

    @Test
    public void fallbackSchedulerShouldHaveAspectScopedThreadName() throws Exception {
        ScheduledExecutorService executor = extractExecutorService(aspect);

        String threadName = executor
            .submit(() -> Thread.currentThread().getName())
            .get(5, TimeUnit.SECONDS);

        assertThat(threadName).contains(EXPECTED_POOL_NAME);
        if (threadType == ThreadType.VIRTUAL) {
            assertThat(threadName).contains("-vthread-");
        } else {
            assertThat(threadName).contains("-thread-");
        }
    }

    private static ScheduledExecutorService extractExecutorService(TimeLimiterAspect aspect)
        throws Exception {
        Field field = TimeLimiterAspect.class.getDeclaredField("timeLimiterExecutorService");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(aspect);
    }
}
