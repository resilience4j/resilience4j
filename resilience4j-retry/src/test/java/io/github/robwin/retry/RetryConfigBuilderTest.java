/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.robwin.retry;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryConfigBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void zeroMaxAttemptsShouldFail() {
        RetryConfig.custom().maxAttempts(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWaitIntervalShouldFail() {
        RetryConfig.custom().waitDuration(Duration.ofMillis(0)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void WaitIntervalUnderTenMillisShouldFail() {
        RetryConfig.custom().waitDuration(Duration.ofMillis(5)).build();
    }

    @Test
    public void waitIntervalOfTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofMillis(10)).build();
        Assertions.assertThat(config).isNotNull();
    }

    @Test
    public void waitIntervalOverTenMillisShouldSucceed() {
        RetryConfig config = RetryConfig.custom().waitDuration(Duration.ofSeconds(10)).build();
        Assertions.assertThat(config).isNotNull();
    }
}
