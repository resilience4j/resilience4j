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
package io.github.resilience4j.retry.monitoring.endpoint;

import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventDTO;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventDTOFactory;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryEventDTOFactoryTest {

    @Test
    void shouldMapRetryOnSuccessEvent() {
        RetryOnSuccessEvent event = new RetryOnSuccessEvent("name", 1,
            new IOException("Error Message"));

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isOne();
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.SUCCESS);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapRetryOnErrorEvent() {
        RetryOnErrorEvent event = new RetryOnErrorEvent("name", 1,
            new IOException("Error Message"));

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isOne();
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.ERROR);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapRetryOnIgnoredErrorEvent() {
        RetryOnIgnoredErrorEvent event = new RetryOnIgnoredErrorEvent("name",
            new IOException("Error Message"));

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isZero();
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.IGNORED_ERROR);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapRetryOnRetryEvent() {
        RetryOnRetryEvent event = new RetryOnRetryEvent("name", 1, new IOException("Error Message"),
            5000);

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isOne();
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.RETRY);
        assertThat(retryEventDTO.getErrorMessage()).isEqualTo("java.io.IOException: Error Message");
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapRetryOnRetryEventWithoutThrowable() {
        RetryOnRetryEvent event = new RetryOnRetryEvent("name", 1, null, 5000);

        RetryEventDTO retryEventDTO = RetryEventDTOFactory.createRetryEventDTO(event);

        assertThat(retryEventDTO.getRetryName()).isEqualTo("name");
        assertThat(retryEventDTO.getNumberOfAttempts()).isOne();
        assertThat(retryEventDTO.getType()).isEqualTo(RetryEvent.Type.RETRY);
        assertThat(retryEventDTO.getErrorMessage()).isNull();
        assertThat(retryEventDTO.getCreationTime()).isNotNull();
    }
}

