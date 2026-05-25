/*
 *
 *  Copyright 2021: Matthew Sandoz
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
package io.github.resilience4j.hedge;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.hedge.event.*;
import io.github.resilience4j.hedge.internal.HedgeImpl;

import java.time.Duration;
import java.util.Map;

/**
 * A Hedge executes extra requests if the main execution does not return in time. Hedges rely on a scheduled thread pool
 * executor, which will queue hedging executions in its priority queue and process them when there are threads available.
 * <p>
 * Set the size limit on your scheduled thread pool executor to limit the number of hedged requests actively running at
 * the same time. You can adjust your rejected execution handler to the desired behavior, such as
 * ThreadPoolExecutor.AbortPolicy.
 * <p>
 * Good candidates for Hedged calls include side-effect-free calls, calls which may have a long response time tail. Do
 * not hedge non-idempotent inserts or other similar calls.
 */
public interface Hedge extends GenericHedge {

    String DEFAULT_NAME = "UNDEFINED";

    /**
     * Creates a Hedge with a default HedgeConfig configuration.
     *
     * @return The Hedge
     */
    static Hedge ofDefaults() {
        return new HedgeImpl(DEFAULT_NAME, HedgeConfig.ofDefaults());
    }

    /**
     * Creates a Hedge with a default HedgeConfig configuration.
     *
     * @param name the name of the Hedge
     * @return The Hedge
     */
    static Hedge ofDefaults(String name) {
        return new HedgeImpl(name, HedgeConfig.ofDefaults());
    }

    /**
     * Creates a Hedge with a HedgeConfig configuration.
     *
     * @param hedgeConfig the HedgeConfig
     * @return The Hedge
     */
    static Hedge of(HedgeConfig hedgeConfig) {
        return of(DEFAULT_NAME, hedgeConfig);
    }

    /**
     * Creates a Hedge with a HedgeConfig configuration.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig the HedgeConfig
     * @return The Hedge
     */
    static Hedge of(String name, HedgeConfig hedgeConfig) {
        return new HedgeImpl(name, hedgeConfig);
    }

    /**
     * Creates a Hedge with a custom Hedge configuration.
     * <p>
     * The {@code tags} passed will be appended to the tags already configured for the registry.
     * When tags (keys) of the two collide the tags passed with this method will override the tags
     * of the registry.
     *
     * @param name        the name of the Hedge
     * @param hedgeConfig a custom Hedge configuration
     * @param tags        tags added to the Hedge
     * @return a Hedge with a custom Hedge configuration.
     */
    static Hedge of(String name, HedgeConfig hedgeConfig,
                    Map<String, String> tags) {
        return new HedgeImpl(name, hedgeConfig, tags);
    }

    /**
     * Creates a Hedge decorator with a timeout Duration.
     *
     * @param hedgeDuration the timeout Duration
     * @return The Hedge
     */
    static Hedge of(Duration hedgeDuration) {
        HedgeConfig hedgeConfig = new HedgeConfig.Builder()
            .preconfiguredDuration(hedgeDuration)
            .build();
        return new HedgeImpl(DEFAULT_NAME, hedgeConfig);
    }

    /**
     * Get the Metrics of this Hedge.
     *
     * @return the Metrics of this Hedge
     */
    Metrics getMetrics();

    interface Metrics {

        /**
         * Returns the number of parallel executions this bulkhead can support at this point in
         * time.
         *
         * @return remaining bulkhead depth
         */
        Duration getCurrentHedgeDelay();
        long getPrimarySuccessCount();
        long getSecondarySuccessCount();
        long getPrimaryFailureCount();
        long getSecondaryFailureCount();
        int getSecondaryPoolActiveCount();
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher extends io.github.resilience4j.core.EventPublisher<HedgeEvent> {

        EventPublisher onPrimarySuccess(EventConsumer<HedgeOnPrimarySuccessEvent> eventConsumer);

        EventPublisher onPrimaryFailure(EventConsumer<HedgeOnPrimaryFailureEvent> eventConsumer);

        EventPublisher onSecondarySuccess(EventConsumer<HedgeOnSecondarySuccessEvent> eventConsumer);

        EventPublisher onSecondaryFailure(EventConsumer<HedgeOnSecondaryFailureEvent> eventConsumer);
    }
}
