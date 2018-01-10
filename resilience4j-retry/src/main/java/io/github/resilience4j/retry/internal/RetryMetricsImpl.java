/*
 *
 *  Copyright 2018 Jan Sykora at GoodData(R) Corporation
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

package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

public class RetryMetricsImpl implements Retry.Metrics {
    private final LongAdder succeededAfterRetryCounter;
    private final LongAdder failedAfterRetryCounter;
    private final LongAdder succeededWithoutRetryCounter;
    private final LongAdder failedWithoutRetryCounter;

    public RetryMetricsImpl(LongAdder succeededAfterRetryCounter,
                            LongAdder failedAfterRetryCounter,
                            LongAdder succeededWithoutRetryCounter,
                            LongAdder failedWithoutRetryCounter) {
        this.succeededAfterRetryCounter = Objects.requireNonNull(succeededAfterRetryCounter);
        this.failedAfterRetryCounter = Objects.requireNonNull(failedAfterRetryCounter);
        this.succeededWithoutRetryCounter = Objects.requireNonNull(succeededWithoutRetryCounter);
        this.failedWithoutRetryCounter = Objects.requireNonNull(failedWithoutRetryCounter);
    }

    @Override
    public long getNumberOfSuccessfulCallsWithoutRetryAttempt() {
        return succeededWithoutRetryCounter.longValue();
    }

    @Override
    public long getNumberOfFailedCallsWithoutRetryAttempt() {
        return failedWithoutRetryCounter.longValue();
    }

    @Override
    public long getNumberOfSuccessfulCallsWithRetryAttempt() {
        return succeededAfterRetryCounter.longValue();
    }

    @Override
    public long getNumberOfFailedCallsWithRetryAttempt() {
        return failedAfterRetryCounter.longValue();
    }
}
