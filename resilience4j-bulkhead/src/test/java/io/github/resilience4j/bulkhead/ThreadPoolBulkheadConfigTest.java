/*
 *
 *  Copyright 2026 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.core.ContextPropagator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThreadPoolBulkheadConfigTest {

    @Test
    void buildCustom() {
        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(maxThreadPoolSize)
            .coreThreadPoolSize(coreThreadPoolSize)
            .queueCapacity(queueCapacity)
            .keepAliveDuration(Duration.ofMillis(maxWait))
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getMaxThreadPoolSize()).isEqualTo(maxThreadPoolSize);
        assertThat(config.getCoreThreadPoolSize()).isEqualTo(coreThreadPoolSize);
        Assertions.assertThat(config.getKeepAliveDuration()).hasMillis(maxWait);
        assertThat(config.getQueueCapacity()).isEqualTo(queueCapacity);
        assertThat(config.getContextPropagator()).isEmpty();

    }

    @Test
    void createFromBaseConfig() {
        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig
            .from(ThreadPoolBulkheadConfig.custom().build())
            .maxThreadPoolSize(maxThreadPoolSize)
            .coreThreadPoolSize(coreThreadPoolSize)
            .queueCapacity(queueCapacity)
            .keepAliveDuration(Duration.ofMillis(maxWait))
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getMaxThreadPoolSize()).isEqualTo(maxThreadPoolSize);
        assertThat(config.getCoreThreadPoolSize()).isEqualTo(coreThreadPoolSize);
        Assertions.assertThat(config.getKeepAliveDuration()).hasMillis(maxWait);
        assertThat(config.getQueueCapacity()).isEqualTo(queueCapacity);
        assertThat(config.getContextPropagator()).isEmpty();
    }

    @Test
    void buildWithIllegalMaxThreadPoolSize() {
        assertThatThrownBy(() -> ThreadPoolBulkheadConfig.custom().maxThreadPoolSize(-1).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithIllegalCoreThreadPoolSize() {
        assertThatThrownBy(() -> ThreadPoolBulkheadConfig.custom().coreThreadPoolSize(-1).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithIllegalMaxWait() {
        assertThatThrownBy(() -> ThreadPoolBulkheadConfig.custom().keepAliveDuration(Duration.ofMillis(-1)).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithIllegalQueueCapacity() {
        assertThatThrownBy(() -> ThreadPoolBulkheadConfig.custom().queueCapacity(-1).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithIllegalMaxCoreThreads() {
        assertThatThrownBy(() -> ThreadPoolBulkheadConfig.custom().maxThreadPoolSize(1).coreThreadPoolSize(2).build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contextPropagatorConfig() {

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig
            .custom()
            .contextPropagator(TestCtxPropagator.class)
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getContextPropagator()).isNotNull();
        assertThat(config.getContextPropagator()).hasSize(1);
        assertThat(config.getContextPropagator().get(0).getClass()).isEqualTo(TestCtxPropagator.class);
    }

    @Test
    void contextPropagatorConfigDefault() {

        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(maxThreadPoolSize)
            .coreThreadPoolSize(coreThreadPoolSize)
            .queueCapacity(queueCapacity)
            .keepAliveDuration(Duration.ofMillis(maxWait))
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getContextPropagator()).isNotNull();
        assertThat(config.getContextPropagator()).isEmpty();
    }

    @Test
    void contextPropagatorSetAsBean() {

        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(maxThreadPoolSize)
            .coreThreadPoolSize(coreThreadPoolSize)
            .queueCapacity(queueCapacity)
            .keepAliveDuration(Duration.ofMillis(maxWait))
            .contextPropagator(new TestCtxPropagator())
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getContextPropagator()).isNotNull();
        assertThat(config.getContextPropagator()).hasSize(1);
        assertThat(config.getContextPropagator().get(0).getClass()).isEqualTo(TestCtxPropagator.class);
    }

    @Test
    void contextPropagatorSetAsBeanOverrideSetAsClass() {

        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(maxThreadPoolSize)
            .coreThreadPoolSize(coreThreadPoolSize)
            .queueCapacity(queueCapacity)
            .keepAliveDuration(Duration.ofMillis(maxWait))
            .contextPropagator(TestCtxPropagator2.class)
            //this should override TestCtxPropagator2 context propagator
            .contextPropagator(new TestCtxPropagator())
            .build();

        assertThat(config).isNotNull();
        assertThat(config.getContextPropagator()).isNotNull();
        assertThat(config.getContextPropagator()).hasSize(2);
        List<Class<? extends ContextPropagator>> ctxPropagators = config.getContextPropagator()
            .stream().map(ct -> ct.getClass()).collect(Collectors.toList());
        assertThat(ctxPropagators).containsExactlyInAnyOrder(TestCtxPropagator.class, TestCtxPropagator2.class);

    }

    @Test
    void testToString() {
        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(maxThreadPoolSize)
            .coreThreadPoolSize(coreThreadPoolSize)
            .queueCapacity(queueCapacity)
            .keepAliveDuration(Duration.ofMillis(maxWait))
            .writableStackTraceEnabled(false)
            .contextPropagator(TestCtxPropagator2.class)
            .build();

        String result = config.toString();
        assertThat(result)
                .startsWith("ThreadPoolBulkheadConfig{")
                .contains("maxThreadPoolSize=20")
                .contains("coreThreadPoolSize=2")
                .contains("queueCapacity=50")
                .contains("keepAliveDuration=PT0.555S")
                .contains("writableStackTraceEnabled=false")
                .contains("contextPropagators=[io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfigTest$TestCtxPropagator2")
                .endsWith("}");
    }

    public static class TestCtxPropagator implements ContextPropagator<Object> {

        @Override
        public Supplier<Optional<Object>> retrieve() {
            return null;
        }

        @Override
        public Consumer<Optional<Object>> copy() {
            return null;
        }

        @Override
        public Consumer<Optional<Object>> clear() {
            return null;
        }
    }

    public static class TestCtxPropagator2 implements ContextPropagator<Object> {

        @Override
        public Supplier<Optional<Object>> retrieve() {
            return null;
        }

        @Override
        public Consumer<Optional<Object>> copy() {
            return null;
        }

        @Override
        public Consumer<Optional<Object>> clear() {
            return null;
        }
    }

}
