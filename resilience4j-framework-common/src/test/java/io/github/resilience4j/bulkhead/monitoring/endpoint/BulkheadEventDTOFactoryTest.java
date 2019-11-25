package io.github.resilience4j.bulkhead.monitoring.endpoint;

import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTOFactory;
import org.junit.Test;

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BulkheadEventDTOFactoryTest {

    @Test
    public void shouldMapBulkheadOnCallFinishedEvent() {
        BulkheadOnCallFinishedEvent event = new BulkheadOnCallFinishedEvent("name");

        BulkheadEventDTO eventDTO = BulkheadEventDTOFactory.createBulkheadEventDTO(event);

        assertThat(eventDTO.getBulkheadName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(CALL_FINISHED);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapBulkheadOnCallPermittedEvent() {
        BulkheadOnCallPermittedEvent event = new BulkheadOnCallPermittedEvent("name");

        BulkheadEventDTO eventDTO = BulkheadEventDTOFactory.createBulkheadEventDTO(event);

        assertThat(eventDTO.getBulkheadName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(CALL_PERMITTED);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapBulkheadOnCallRejectedEvent() {
        BulkheadOnCallRejectedEvent event = new BulkheadOnCallRejectedEvent("name");

        BulkheadEventDTO eventDTO = BulkheadEventDTOFactory.createBulkheadEventDTO(event);

        assertThat(eventDTO.getBulkheadName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(CALL_REJECTED);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }
}

