package io.github.resilience4j.retry.monitoring.endpoint;

import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventDTO;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventDTOFactory;
import io.github.resilience4j.retry.event.*;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryEventDTOFactoryTest {

    @Test
    public void shouldMapRetryOnSuccessEvent() {
        RetryOnSuccessEvent event = new RetryOnSuccessEvent("name", 1,
            new IOException("Error Message"));

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isEqualTo(1);
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.SUCCESS);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapRetryOnErrorEvent() {
        RetryOnErrorEvent event = new RetryOnErrorEvent("name", 1,
            new IOException("Error Message"));

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isEqualTo(1);
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.ERROR);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapRetryOnIgnoredErrorEvent() {
        RetryOnIgnoredErrorEvent event = new RetryOnIgnoredErrorEvent("name",
            new IOException("Error Message"));

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isEqualTo(0);
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.IGNORED_ERROR);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    public void shouldMapRetryOnRetryEvent() {
        RetryOnRetryEvent event = new RetryOnRetryEvent("name", 1, new IOException("Error Message"),
            5000);

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isEqualTo(1);
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.RETRY);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }
}

