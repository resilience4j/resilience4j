/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.timelimiter.monitoring.endpoint;

import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventDTO;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class TimeLimiterEventDTOTest {

    @Test
    void shouldMapTimeLimiterOnSuccessEvent(){
        TimeLimiterOnSuccessEvent event = new TimeLimiterOnSuccessEvent("name");

        TimeLimiterEventDTO timeLimiterEventDTO = TimeLimiterEventDTO.createTimeLimiterEventDTO(event);

        assertThat(timeLimiterEventDTO.getTimeLimiterName()).isEqualTo("name");
        assertThat(timeLimiterEventDTO.getType()).isEqualTo(TimeLimiterEvent.Type.SUCCESS);
        assertThat(timeLimiterEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapTimeLimiterOnErrorEvent(){
        TimeLimiterOnErrorEvent event = new TimeLimiterOnErrorEvent("name", new IOException("Error message"));

        TimeLimiterEventDTO timeLimiterEventDTO = TimeLimiterEventDTO.createTimeLimiterEventDTO(event);

        assertThat(timeLimiterEventDTO.getTimeLimiterName()).isEqualTo("name");
        assertThat(timeLimiterEventDTO.getType()).isEqualTo(TimeLimiterEvent.Type.ERROR);
        assertThat(timeLimiterEventDTO.getCreationTime()).isNotNull();
    }


    @Test
    void shouldMapTimeLimiterOnTimeoutEvent(){
        TimeLimiterOnTimeoutEvent event = new TimeLimiterOnTimeoutEvent("name");

        TimeLimiterEventDTO timeLimiterEventDTO = TimeLimiterEventDTO.createTimeLimiterEventDTO(event);

        assertThat(timeLimiterEventDTO.getTimeLimiterName()).isEqualTo("name");
        assertThat(timeLimiterEventDTO.getType()).isEqualTo(TimeLimiterEvent.Type.TIMEOUT);
        assertThat(timeLimiterEventDTO.getCreationTime()).isNotNull();
    }
}