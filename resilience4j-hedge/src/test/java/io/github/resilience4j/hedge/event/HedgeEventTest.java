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
package io.github.resilience4j.hedge.event;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

public class HedgeEventTest {
    private static final String HEDGE_NAME = "HEDGE_NAME";
    private static final Duration ONE_SECOND = Duration.ofMillis(1000);
    private static final Throwable THROWABLE = new Throwable(HEDGE_NAME);
    public static final String SUCCESSFUL_HEDGE_TEXT = "Hedge 'HEDGE_NAME' recorded successful hedged call in 1000ms";
    public static final String FAILED_HEDGE_TEXT = "Hedge 'HEDGE_NAME' recorded an error: 'java.lang.Throwable: HEDGE_NAME' in 1000ms";
    public static final String SUCCESSFUL_PRIMARY_TEXT = "Hedge 'HEDGE_NAME' recorded a successful call in 1000ms";
    public static final String FAILED_PRIMARY_TEXT = "Hedge 'HEDGE_NAME' recorded an error: 'java.lang.Throwable: HEDGE_NAME' in 1000ms";

    @Test
    public void shouldCreateCorrectHedgeSuccess() {
        HedgeSuccessEvent event = new HedgeSuccessEvent(HEDGE_NAME, ONE_SECOND);

        then(event.getEventType()).isEqualTo(HedgeEvent.Type.HEDGE_SUCCESS);
        then(event.getHedgeName()).isEqualTo(HEDGE_NAME);
        then(event.getDuration()).isEqualTo(ONE_SECOND);
        then(event.toString()).contains(SUCCESSFUL_HEDGE_TEXT);
    }

    @Test
    public void shouldCreateCorrectHedgeFailure() {
        HedgeFailureEvent event = new HedgeFailureEvent(HEDGE_NAME, ONE_SECOND, THROWABLE);

        then(event.getEventType()).isEqualTo(HedgeEvent.Type.HEDGE_FAILURE);
        then(event.getHedgeName()).isEqualTo(HEDGE_NAME);
        then(event.getDuration()).isEqualTo(ONE_SECOND);
        then(event.getThrowable()).isEqualTo(THROWABLE);
        then(event.toString()).contains(FAILED_HEDGE_TEXT);
    }

    @Test
    public void shouldCreateCorrectPrimarySuccess() {
        PrimarySuccessEvent event = new PrimarySuccessEvent(HEDGE_NAME, ONE_SECOND);

        then(event.getEventType()).isEqualTo(HedgeEvent.Type.PRIMARY_SUCCESS);
        then(event.getHedgeName()).isEqualTo(HEDGE_NAME);
        then(event.getDuration()).isEqualTo(ONE_SECOND);
        then(event.toString()).contains(SUCCESSFUL_PRIMARY_TEXT);
    }

    @Test
    public void shouldCreateCorrectPrimaryFailure() {
        PrimaryFailureEvent event = new PrimaryFailureEvent(HEDGE_NAME, ONE_SECOND, THROWABLE);

        then(event.getEventType()).isEqualTo(HedgeEvent.Type.PRIMARY_FAILURE);
        then(event.getHedgeName()).isEqualTo(HEDGE_NAME);
        then(event.getDuration()).isEqualTo(ONE_SECOND);
        then(event.getThrowable()).isEqualTo(THROWABLE);
        then(event.toString()).contains(FAILED_PRIMARY_TEXT);
    }

}