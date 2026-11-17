/*
 *
 *  Copyright 2026
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
package io.github.resilience4j.ratelimiter.event;

import io.github.resilience4j.ratelimiter.event.RateLimiterEvent.Type;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterEventTest {

    @Test
    void rateLimiterOnSuccessEvent() {
        RateLimiterOnSuccessEvent event = new RateLimiterOnSuccessEvent("test", 5);

        assertThat(event.getRateLimiterName()).isEqualTo("test");
        assertThat(event.getEventType()).isEqualTo(Type.SUCCESSFUL_ACQUIRE);
        assertThat(event.getCreationTime()).isNotNull();
        assertThat(event.getNumberOfPermits()).isEqualTo(5);
        assertThat(event.toString())
            .contains("type=SUCCESSFUL_ACQUIRE", "rateLimiterName='test'", "numberOfPermits=5");
    }

    @Test
    void rateLimiterOnFailureEvent() {
        RateLimiterOnFailureEvent event = new RateLimiterOnFailureEvent("test", 7);

        assertThat(event.getRateLimiterName()).isEqualTo("test");
        assertThat(event.getEventType()).isEqualTo(Type.FAILED_ACQUIRE);
        assertThat(event.getCreationTime()).isNotNull();
        assertThat(event.getNumberOfPermits()).isEqualTo(7);
        assertThat(event.toString())
            .contains("type=FAILED_ACQUIRE", "rateLimiterName='test'", "numberOfPermits=7");
    }

    @Test
    void rateLimiterOnDrainedEvent() {
        RateLimiterOnDrainedEvent event = new RateLimiterOnDrainedEvent("test", 3);

        assertThat(event.getRateLimiterName()).isEqualTo("test");
        assertThat(event.getEventType()).isEqualTo(Type.DRAINED);
        assertThat(event.getCreationTime()).isNotNull();
        assertThat(event.getNumberOfPermits()).isEqualTo(3);
        assertThat(event.toString())
            .contains("type=DRAINED", "rateLimiterName='test'", "numberOfPermits=3");
    }

    @Test
    void rateLimiterEventWithDefaultNumberOfPermits() {
        RateLimiterOnSuccessEvent event = new RateLimiterOnSuccessEvent("test");

        assertThat(event.getNumberOfPermits()).isEqualTo(1);
        assertThat(event.toString()).contains("numberOfPermits=1");
    }
}
