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
     * Returns the current total duration of all calls.
     *
     * @return the current total duration of all calls
     */
    Duration getTotalDuration();

    /**
     * Returns the current average duration of all calls.
     *
     * @return the current average duration of all calls
     */
    Duration getAverageDuration();

    /**
     * Returns the current number of calls which were slower than a certain threshold.
     *
     * @return the current number of calls which were slower than a certain threshold
     */
    int getTotalNumberOfSlowCalls();

    /**
     * Returns the current number of successful calls which were slower than a certain threshold.
     *
     * @return the current number of successful calls which were slower than a certain threshold
     */
    int getNumberOfSlowSuccessfulCalls();

    /**
     * Returns the current number of failed calls which were slower than a certain threshold.
     *
     * @return the current number of failed calls which were slower than a certain threshold
     */
    int getNumberOfSlowFailedCalls();

    /**
     * Returns the current percentage of calls which were slower than a certain threshold.
     *
     * @return the current percentage of calls which were slower than a certain threshold
     */
    float getSlowCallRate();

    /**
     * Returns the current number of successful calls.
     *
     * @return the current number of successful calls
     */
    int getNumberOfSuccessfulCalls();

    /**
     * Returns the current number of failed calls.
     *
     * @return the current number of failed calls
     */
    int getNumberOfFailedCalls();

    /**
     * Returns the current total number of all calls.
     *
     * @return the current total number of all calls
     */
    int getTotalNumberOfCalls();

    /**
     * Returns the current failure rate in percentage.
     *
     * @return the current  failure rate in percentage
     */
    float getFailureRate();
}
