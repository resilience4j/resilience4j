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
package javaslang.circuitbreaker;

/**
 * A CircuitBreakerEvent which informs about a state transition.
 */
public class CircuitBreakerStateTransitionEvent implements CircuitBreakerEvent{

    private String circuitBreakerName;
    private CircuitBreaker.StateTransition stateTransition;

    public CircuitBreakerStateTransitionEvent(String circuitBreakerName, CircuitBreaker.StateTransition stateTransition) {
        this.circuitBreakerName = circuitBreakerName;
        this.stateTransition = stateTransition;
    }

    public CircuitBreaker.StateTransition getStateTransition() {
        return stateTransition;
    }

    @Override
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    @Override
    public Type getEventType() {
        return Type.STATE_TRANSITION;
    }

    @Override
    public String toString(){
        return String.format("CircuitBreaker '%s' changes state from %s to %s", getCircuitBreakerName(), getStateTransition().getFromState(), getStateTransition().getToState());

    }
}
