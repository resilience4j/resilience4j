package io.github.resilience4j.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxRetriesExceededTest {

    @Test
    void errorMessageShouldReportedRight() {
        MaxRetriesExceeded maxRetriesExceeded = new MaxRetriesExceeded("test max retries");
        assertThat(maxRetriesExceeded.getMessage()).isEqualTo("test max retries");
    }
}
