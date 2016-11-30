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

//@State(Scope.Benchmark)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
//@BenchmarkMode(Mode.Throughput)
//public class CircuitBreakerBenchmark {
//
//    private CircuitBreaker circuitBreaker;
//    private Supplier<String> supplier;
//    private static final int ITERATION_COUNT = 10;
//    private static final int WARMUP_COUNT = 10;
//    private static final int THREAD_COUNT = 10;
//
//    @Setup
//    public void setUp() {
//        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
//                .failureRateThreshold(1)
//                .waitDurationInOpenState(Duration.ofSeconds(1))
//                .build());
//        circuitBreaker = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");
//
//        supplier = CircuitBreaker.decorateSupplier(() -> {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "Hello Benchmark";
//        }, circuitBreaker);
//    }
//
//    @Benchmark
//    @Threads(value = THREAD_COUNT)
//    @Warmup(iterations = WARMUP_COUNT)
//    @Measurement(iterations = ITERATION_COUNT)
//    public String invokeSupplier(){
//        return supplier.get();
//    }
//}
