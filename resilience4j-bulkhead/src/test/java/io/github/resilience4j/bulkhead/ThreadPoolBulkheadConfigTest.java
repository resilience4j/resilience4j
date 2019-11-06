/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadPoolBulkheadConfigTest {

    @Test
    public void testBuildCustom() {
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
        assertThat(config.getKeepAliveDuration().toMillis()).isEqualTo(maxWait);
        assertThat(config.getQueueCapacity()).isEqualTo(queueCapacity);
    }

    @Test
    public void testCreateFromBaseConfig() {
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
        assertThat(config.getKeepAliveDuration().toMillis()).isEqualTo(maxWait);
        assertThat(config.getQueueCapacity()).isEqualTo(queueCapacity);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxThreadPoolSize() {
        ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(-1)
            .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalCoreThreadPoolSize() {
        ThreadPoolBulkheadConfig.custom()
            .coreThreadPoolSize(-1)
            .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxWait() {
        ThreadPoolBulkheadConfig.custom()
            .keepAliveDuration(Duration.ofMillis(-1))
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalQueueCapacity() {
        ThreadPoolBulkheadConfig.custom()
            .queueCapacity(-1)
            .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxCoreThreads() {
        ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(2)
            .build();
    }

}
