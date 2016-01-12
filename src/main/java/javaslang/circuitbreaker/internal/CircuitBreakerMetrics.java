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

class CircuitBreakerMetrics {

    private final RingBitSet ringBitSet;

    /**
     * Maximum number of buffered calls
     */
    private int maxNumberOfBufferedCalls;

    CircuitBreakerMetrics(int ringBufferSize) {
        this.ringBitSet = new RingBitSet(ringBufferSize);
        this.maxNumberOfBufferedCalls = ringBufferSize;
    }

    /**
     * Records a failed call and returns the current failure rate in percentage.
     *
     * @return the current failure rate  in percentage.
     */
    public synchronized float recordFailure(){
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

    /**
     * Returns the failure rate in percentage. If the number of measured calls is below the minimum number of measured calls,
     * it returns -1.
     *
     * @return the failure rate in percentage
     */
    public synchronized float getFailureRate(){
        int numOfMeasuredCalls = getCurrentNumberOfBufferedCalls();
        if(numOfMeasuredCalls == maxNumberOfBufferedCalls){
            return getCurrentNumberOfFailedCalls() * 100.0f / numOfMeasuredCalls;
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

    /**
     * Returns the current number of buffered calls.
     *
     * @return he current number of buffered calls
     */
    public synchronized int getCurrentNumberOfBufferedCalls() {
        return this.ringBitSet.length();
    }

    /**
     * Returns the current number of failed calls.
     *
     * @return the current number of failed calls.
     */
    public synchronized int getCurrentNumberOfFailedCalls() {
        return this.ringBitSet.cardinality();
    }
}
