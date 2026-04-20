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
package io.github.resilience4j.bulkhead.monitoring.endpoint;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTOFactory;

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_FINISHED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_PERMITTED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

class BulkheadEventDTOFactoryTest {

    @Test
    void shouldMapBulkheadOnCallFinishedEvent() {
        BulkheadOnCallFinishedEvent event = new BulkheadOnCallFinishedEvent("name");

        BulkheadEventDTO eventDTO = BulkheadEventDTOFactory.createBulkheadEventDTO(event);

        assertThat(eventDTO.getBulkheadName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(CALL_FINISHED);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapBulkheadOnCallPermittedEvent() {
        BulkheadOnCallPermittedEvent event = new BulkheadOnCallPermittedEvent("name");

        BulkheadEventDTO eventDTO = BulkheadEventDTOFactory.createBulkheadEventDTO(event);

        assertThat(eventDTO.getBulkheadName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(CALL_PERMITTED);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapBulkheadOnCallRejectedEvent() {
        BulkheadOnCallRejectedEvent event = new BulkheadOnCallRejectedEvent("name");

        BulkheadEventDTO eventDTO = BulkheadEventDTOFactory.createBulkheadEventDTO(event);

        assertThat(eventDTO.getBulkheadName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(CALL_REJECTED);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }
}

