/*
 * Copyright 2019 Andrew From
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

package io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CircuitBreakerMetricsDTO {

    private float failureRate;
    private int numberOfBufferedCalls;
    private int numberOfFailedCalls;
    private long numberOfNotPermittedCalls;
    private int numberOfSuccessfulCalls;

    CircuitBreakerMetricsDTO() {
    }

    public CircuitBreakerMetricsDTO(CircuitBreaker.Metrics metrics) {
        this.failureRate = metrics.getFailureRate();
        this.numberOfBufferedCalls = metrics.getNumberOfBufferedCalls();
        this.numberOfFailedCalls = metrics.getNumberOfFailedCalls();
        this.numberOfNotPermittedCalls = metrics.getNumberOfNotPermittedCalls();
        this.numberOfSuccessfulCalls = metrics.getNumberOfSuccessfulCalls();
    }

    public float getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(float failureRate) {
        this.failureRate = failureRate;
    }

    public int getNumberOfBufferedCalls() {
        return numberOfBufferedCalls;
    }

    public void setNumberOfBufferedCalls(int numberOfBufferedCalls) {
        this.numberOfBufferedCalls = numberOfBufferedCalls;
    }

    public int getNumberOfFailedCalls() {
        return numberOfFailedCalls;
    }

    public void setNumberOfFailedCalls(int numberOfFailedCalls) {
        this.numberOfFailedCalls = numberOfFailedCalls;
    }

    public long getNumberOfNotPermittedCalls() {
        return numberOfNotPermittedCalls;
    }

    public void setNumberOfNotPermittedCalls(long numberOfNotPermittedCalls) {
        this.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
    }

    public int getNumberOfSuccessfulCalls() {
        return numberOfSuccessfulCalls;
    }

    public void setNumberOfSuccessfulCalls(int numberOfSuccessfulCalls) {
        this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
    }
}
