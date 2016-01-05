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
package io.github.robwin.circuitbreaker;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.function.Supplier;

@State(Scope.Benchmark)
public class CircuitBreakerBenchmark {

    private CircuitBreaker circuitBreaker;
    private Supplier<String> supplier;

    @Setup
    public void setUp() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .maxFailures(1)
                .waitInterval(1000)
                .build());
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");

        supplier = CircuitBreaker.decorateSupplier(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Hello Benchmark";
        }, circuitBreaker);
    }

    @Benchmark
    public void invokeSupplier(){
        supplier.get();
    }
}
