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

import org.junit.Test;

import java.io.IOException;

import static io.github.robwin.retry.event.RetryEvent.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RetryEventTest {

    @Test
    public void testRetryOnErrorEvent() {
        RetryOnErrorEvent retryOnErrorEvent = new RetryOnErrorEvent("test", 2,
                new IOException());
        assertThat(retryOnErrorEvent.getId()).isEqualTo("test");
        assertThat(retryOnErrorEvent.getNumberOfAttempts()).isEqualTo(2);
        assertThat(retryOnErrorEvent.getEventType()).isEqualTo(Type.ERROR);
        assertThat(retryOnErrorEvent.getLastThrowable()).isInstanceOf(IOException.class);
    }

    @Test
    public void testRetryOnSuccessEvent() {
        RetryOnSuccessEvent retryOnSuccessEvent = new RetryOnSuccessEvent("test", 2,
                new IOException());
        assertThat(retryOnSuccessEvent.getId()).isEqualTo("test");
        assertThat(retryOnSuccessEvent.getNumberOfAttempts()).isEqualTo(2);
        assertThat(retryOnSuccessEvent.getEventType()).isEqualTo(Type.SUCCESS);
        assertThat(retryOnSuccessEvent.getLastThrowable()).isInstanceOf(IOException.class);
    }

}
