/*
 * Copyright 2020 Mahmoud Romeh
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
package io.github.resilience4j.common.circuitbreaker.monitoring.endpoint;

import java.util.Objects;

/**
 * change circuit breaker state response
 */
public class CircuitBreakerUpdateStateResponse {
    private String circuitBreakerName;
    private String currentState;
    private String message;

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public void setCircuitBreakerName(String circuitBreakerName) {
        this.circuitBreakerName = circuitBreakerName;
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String currentState) {
        this.currentState = currentState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircuitBreakerUpdateStateResponse that = (CircuitBreakerUpdateStateResponse) o;
        return circuitBreakerName.equals(that.circuitBreakerName) &&
            currentState.equals(that.currentState) &&
            message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(circuitBreakerName, currentState, message);
    }

    @Override
    public String toString() {
        return "CircuitBreakerUpdateStateResponse{" +
            "circuitBreakerName='" + circuitBreakerName + '\'' +
            ", currentState='" + currentState + '\'' +
            ", message='" + message + '\'' +
            '}';
    }
}
