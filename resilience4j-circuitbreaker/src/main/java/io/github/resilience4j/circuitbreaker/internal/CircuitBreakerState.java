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
package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;

/**
 * Abstract state of the CircuitBreaker state machine.
 */
abstract class CircuitBreakerState{

    CircuitBreakerStateMachine stateMachine;

    CircuitBreakerState(CircuitBreakerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    abstract boolean tryObtainPermission();

    abstract void obtainPermission();

    abstract void onError(Throwable throwable);

    abstract void onSuccess();

    abstract CircuitBreaker.State getState();

    abstract CircuitBreakerMetrics getMetrics();

    /**
     * Should the CircuitBreaker in this state publish events
     * @return a boolean signaling if the events should be published
     */
    boolean shouldPublishEvents(CircuitBreakerEvent event){
        return event.getEventType().forcePublish || getState().allowPublish;
    }
}
