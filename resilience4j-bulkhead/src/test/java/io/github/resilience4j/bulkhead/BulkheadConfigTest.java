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

import static org.assertj.core.api.Java6Assertions.assertThat;

public class BulkheadConfigTest {


	@Test
	public void testBuildCustomWithDuration() {

		// given
		int maxConcurrent = 66;
		long maxWait = 555;

		// when
		BulkheadConfig config = BulkheadConfig.custom()
				.maxConcurrentCalls(maxConcurrent)
				.maxWaitDuration(Duration.ofMillis(555))
				.build();

		// then
		assertThat(config).isNotNull();
		assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
		assertThat(config.getMaxWaitDuration().toMillis()).isEqualTo(maxWait);
	}

    @Test
    public void testBuildCustomWithWritableStackTraceDisabled() {

        // given
        int maxConcurrent = 66;

        // when
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrent)
                .writableStackTraceEnabled(false)
                .build();

        // then
        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
        assertThat(config.isWritableStackTraceEnabled()).isFalse();
    }

    @Test
    public void testBuildCustom() {

        // given
        int maxConcurrent = 66;
        long maxWait = 555;

        // when
        BulkheadConfig config = BulkheadConfig.custom()
                                              .maxConcurrentCalls(maxConcurrent)
                                              .maxWaitDuration(Duration.ofMillis(maxWait))
                                              .build();

        // then
        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
        assertThat(config.getMaxWaitDuration().toMillis()).isEqualTo(maxWait);
    }

    @Test
    public void testBuildWithZeroMaxCurrentCalls() {

        // given
        int maxConcurrent = 0;

        // when
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrent)
                .build();

        // then
        assertThat(config).isNotNull();
        assertThat(config.getMaxConcurrentCalls()).isEqualTo(maxConcurrent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxConcurrent() {

        // when
        BulkheadConfig.custom()
                      .maxConcurrentCalls(-1)
                      .build();

    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildWithIllegalMaxWait() {

        // when
        BulkheadConfig.custom()
            .maxWaitDuration(Duration.ofMillis(-1))
            .build();
    }

	@Test(expected = IllegalArgumentException.class)
	public void testBuildWithIllegalMaxWaitDuration() {
		// when
		BulkheadConfig.custom()
				.maxWaitDuration(Duration.ofSeconds(-1))
				.build();
	}

}
