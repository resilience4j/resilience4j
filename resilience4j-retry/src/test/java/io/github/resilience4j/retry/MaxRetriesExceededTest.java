package io.github.resilience4j.retry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MaxRetriesExceededTest {


    @Test
    public void errorMessageShouldReportedRight() {
        MaxRetriesExceeded maxRetriesExceeded = new MaxRetriesExceeded("test max retries");
        assertEquals(maxRetriesExceeded.getMessage(), "test max retries");

    }

}