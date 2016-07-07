/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.retry;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void zeroMaxAttemptsShouldFail() {
        Retry.custom().maxAttempts(0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroWaitIntervalShouldFail() {
        Retry.custom().waitDuration(Duration.ofMillis(0)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void WaitIntervalUnderTenMillisShouldFail() {
        Retry.custom().waitDuration(Duration.ofMillis(5)).build();
    }

    @Test
    public void waitIntervalOfTenMillisShouldSucceed() {
        Retry retryCtx = Retry.custom().waitDuration(Duration.ofMillis(10)).build();
        assertThat(retryCtx).isNotNull();
    }

    @Test
    public void waitIntervalOverTenMillisShouldSucceed() {
        Retry retryCtx = Retry.custom().waitDuration(Duration.ofSeconds(10)).build();
        assertThat(retryCtx).isNotNull();
    }
}
