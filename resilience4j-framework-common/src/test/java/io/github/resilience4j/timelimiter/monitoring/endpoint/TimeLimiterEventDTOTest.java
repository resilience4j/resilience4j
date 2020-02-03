package io.github.resilience4j.timelimiter.monitoring.endpoint;

import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventDTO;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;


public class TimeLimiterEventDTOTest {

    @Test
    public void shouldMapTimeLimiterOnSuccessEvent(){
        TimeLimiterOnSuccessEvent event = new TimeLimiterOnSuccessEvent("name");

        TimeLimiterEventDTO timeLimiterEventDTO = TimeLimiterEventDTO.createTimeLimiterEventDTO(event);

        assertThat(timeLimiterEventDTO.getTimeLimiterName()).isEqualTo("name");
        assertThat(timeLimiterEventDTO.getType()).isEqualTo(TimeLimiterEvent.Type.SUCCESS);
        assertThat(timeLimiterEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapTimeLimiterOnErrorEvent(){
        TimeLimiterOnErrorEvent event = new TimeLimiterOnErrorEvent("name", new IOException("Error message"));

        TimeLimiterEventDTO timeLimiterEventDTO = TimeLimiterEventDTO.createTimeLimiterEventDTO(event);

        assertThat(timeLimiterEventDTO.getTimeLimiterName()).isEqualTo("name");
        assertThat(timeLimiterEventDTO.getType()).isEqualTo(TimeLimiterEvent.Type.ERROR);
        assertThat(timeLimiterEventDTO.getCreationTime()).isNotNull();
    }


    @Test
    public void shouldMapTimeLimiterOnTimeoutEvent(){
        TimeLimiterOnTimeoutEvent event = new TimeLimiterOnTimeoutEvent("name");

        TimeLimiterEventDTO timeLimiterEventDTO = TimeLimiterEventDTO.createTimeLimiterEventDTO(event);

        assertThat(timeLimiterEventDTO.getTimeLimiterName()).isEqualTo("name");
        assertThat(timeLimiterEventDTO.getType()).isEqualTo(TimeLimiterEvent.Type.TIMEOUT);
        assertThat(timeLimiterEventDTO.getCreationTime()).isNotNull();
    }
}