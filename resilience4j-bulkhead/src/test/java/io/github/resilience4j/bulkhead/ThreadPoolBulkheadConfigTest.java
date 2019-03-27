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

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

public class ThreadPoolBulkheadConfigTest {

    @Test
    public void testBuildCustom() {

        // given
        int maxThreadPoolSize = 20;
        int coreThreadPoolSize = 2;
        long maxWait = 555;
        int queueCapacity = 50;

        // when
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(maxThreadPoolSize)
                .coreThreadPoolSize(coreThreadPoolSize)
                .queueCapacity(queueCapacity)
                .keepAliveTime(maxWait)
                .build();

        // then
        assertThat(config).isNotNull();
        assertThat(config.getMaxThreadPoolSize()).isEqualTo(maxThreadPoolSize);
        assertThat(config.getCoreThreadPoolSize()).isEqualTo(coreThreadPoolSize);
        assertThat(config.getKeepAliveTime()).isEqualTo(maxWait);
        assertThat(config.getQueueCapacity()).isEqualTo(queueCapacity);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxThreadPoolSize() {
        // when
        ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(-1)
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalCoreThreadPoolSize() {
        // when
        ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(-1)
                .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxWait() {
        // when
        ThreadPoolBulkheadConfig.custom()
                .keepAliveTime(-1)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalQueueCapacity() {
        // when
        ThreadPoolBulkheadConfig.custom()
                .queueCapacity(-1)
                .build();
    }

}
