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
package javaslang.circuitbreaker.internal;


import javaslang.circuitbreaker.CircuitBreaker;

class CircuitBreakerMetrics implements CircuitBreaker.Metrics {

    private final RingBitSet ringBitSet;

    /**
     * Maximum number of buffered calls
     */
    private int maxNumberOfBufferedCalls;

    private int maxNumberOfBufferedExceptions;

    CircuitBreakerMetrics(int ringBufferSize) {
        this.ringBitSet = new RingBitSet(ringBufferSize);
        this.maxNumberOfBufferedCalls = ringBufferSize;
    }

    /**
     * Records a failed call and returns the current failure rate in percentage.
     *
     * @return the current failure rate  in percentage.
     */
    public synchronized float recordFailure(Throwable throwable){
        ringBitSet.setNextBit(true);
        return getFailureRate();
    }

    /**
     * Records a successful call and returns the current failure rate in percentage.
     *
     * @return the current failure rate in percentage.
     */
    public synchronized float recordSuccess(){
        ringBitSet.setNextBit(false);
        return getFailureRate();
    }


    @Override
    public synchronized float getFailureRate(){
        int numOfMeasuredCalls = getNumberOfBufferedCalls();
        if(numOfMeasuredCalls == maxNumberOfBufferedCalls){
            return getNumberOfFailedCalls() * 100.0f / numOfMeasuredCalls;
        }else{
            return -1f;
        }
    }

    /**
     * Returns the maximum number of buffered calls.
     *
     * @return the maximum number of buffered calls
     */
    public long getMaxNumberOfBufferedCalls() {
        return maxNumberOfBufferedCalls;
    }

    @Override
    public synchronized int getNumberOfBufferedCalls() {
        return this.ringBitSet.length();
    }

    @Override
    public synchronized int getNumberOfFailedCalls() {
        return this.ringBitSet.cardinality();
    }
}
