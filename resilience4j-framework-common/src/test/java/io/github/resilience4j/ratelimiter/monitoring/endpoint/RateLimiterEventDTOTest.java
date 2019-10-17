package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventDTO;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import org.junit.Test;

import static io.github.resilience4j.ratelimiter.event.RateLimiterEvent.Type.FAILED_ACQUIRE;
import static io.github.resilience4j.ratelimiter.event.RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE;
import static org.assertj.core.api.Assertions.assertThat;

public class RateLimiterEventDTOTest {

    @Test
    public void shouldMapRateLimiterOnFailureEvent() {
        RateLimiterOnFailureEvent event = new RateLimiterOnFailureEvent("name");

        RateLimiterEventDTO eventDTO = RateLimiterEventDTO.createRateLimiterEventDTO(event);

        assertThat(eventDTO.getRateLimiterName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(FAILED_ACQUIRE);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapRateLimiterOnSuccessEvent() {
        RateLimiterOnSuccessEvent event = new RateLimiterOnSuccessEvent("name");

        RateLimiterEventDTO eventDTO = RateLimiterEventDTO.createRateLimiterEventDTO(event);

        assertThat(eventDTO.getRateLimiterName()).isEqualTo("name");
        assertThat(eventDTO.getType()).isEqualTo(SUCCESSFUL_ACQUIRE);
        assertThat(eventDTO.getCreationTime()).isNotNull();
    }
}
