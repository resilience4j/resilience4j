/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.robwin.retry.event;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

import static io.github.robwin.retry.event.RetryEvent.Type;

public class RetryEventTest {

    @Test
    public void testRetryOnErrorEvent() {
        RetryOnErrorEvent retryOnErrorEvent = new RetryOnErrorEvent("test", 2,
                new IOException());
        Assertions.assertThat(retryOnErrorEvent.getId()).isEqualTo("test");
        Assertions.assertThat(retryOnErrorEvent.getNumberOfAttempts()).isEqualTo(2);
        Assertions.assertThat(retryOnErrorEvent.getEventType()).isEqualTo(Type.ERROR);
        Assertions.assertThat(retryOnErrorEvent.getLastThrowable()).isInstanceOf(IOException.class);
        Assertions.assertThat(retryOnErrorEvent.toString()).contains("Retry 'test' recorded a failed retry attempt. Number of retry attempts: '2', Last exception was: 'java.io.IOException'.");
    }

    @Test
    public void testRetryOnSuccessEvent() {
        RetryOnSuccessEvent retryOnSuccessEvent = new RetryOnSuccessEvent("test", 2,
                new IOException());
        Assertions.assertThat(retryOnSuccessEvent.getId()).isEqualTo("test");
        Assertions.assertThat(retryOnSuccessEvent.getNumberOfAttempts()).isEqualTo(2);
        Assertions.assertThat(retryOnSuccessEvent.getEventType()).isEqualTo(Type.SUCCESS);
        Assertions.assertThat(retryOnSuccessEvent.getLastThrowable()).isInstanceOf(IOException.class);
        Assertions.assertThat(retryOnSuccessEvent.toString()).contains("Retry 'test' recorded a successful retry attempt. Number of retry attempts: '2', Last exception was: 'java.io.IOException'.");
    }

}
