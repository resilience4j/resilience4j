/*
 *
 *  Copyright 2019 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.core.metrics;

import java.time.Duration;

public interface Snapshot {

    /**
     * Returns the total duration of all calls.
     *
     * @return the total duration of all calls
     */
    Duration getTotalDuration();

    /**
     * Returns the average duration of all calls.
     *
     * @return the average duration of all calls
     */
    Duration getAverageDuration();

    /**
     * Returns the total number of calls which were slower than a certain threshold.
     *
     * @return the total number of calls which were slower than a certain threshold
     */
    int getNumberOfSlowCalls();

    /**
     * Returns the percentage of calls which were slower than a certain threshold.
     *
     * @return the percentage of call which were slower than a certain threshold
     */
    float getSlowCallsPercentage();

    /**
     * Returns the total number of successful calls.
     *
     * @return the total number of successful calls
     */
    int getNumberOfSuccessfulCalls();

    /**
     * Returns the total number of failed calls.
     *
     * @return the total number of failed calls
     */
    int getNumberOfFailedCalls();

    /**
     * Returns the total number of all calls.
     *
     * @return the total number of all calls
     */
    int getTotalNumberOfCalls();

    /**
     * Returns the average number of calls per second.
     *
     * @return the average number of calls per second
     */
    float getAverageNumberOfCallsPerSecond();

    /**
     * Returns the failure rate in percentage.
     *
     * @return the failure rate in percentage
     */
    float getFailureRatePercentage();
}
