/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead.event;

import io.github.resilience4j.bulkhead.event.BulkheadEvent.Type;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkheadEventTest {

    @Test
    public void testBulkheadOnCallRejectedEvent() {

        BulkheadOnCallRejectedEvent event = new BulkheadOnCallRejectedEvent("test");

        assertThat(event.getBulkheadName()).isEqualTo("test");
        assertThat(event.getEventType()).isEqualTo(Type.CALL_REJECTED);
        assertThat(event.getCreationTime()).isNotNull();
        assertThat(event.toString()).contains("Bulkhead 'test' rejected a call.");
    }

    @Test
    public void testBulkheadOnCallPermittedEvent() {

        BulkheadOnCallPermittedEvent event = new BulkheadOnCallPermittedEvent("test");

        assertThat(event.getBulkheadName()).isEqualTo("test");
        assertThat(event.getEventType()).isEqualTo(Type.CALL_PERMITTED);
        assertThat(event.getCreationTime()).isNotNull();
        assertThat(event.toString()).contains("Bulkhead 'test' permitted a call.");
    }

}
