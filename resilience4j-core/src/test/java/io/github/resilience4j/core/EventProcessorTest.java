/*
 *
 *  Copyright 2017: Robert Winkler
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
package io.github.resilience4j.core;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class EventProcessorTest {

    private Logger logger;

    @Before
    public void setUp(){
        logger = mock(Logger.class);
    }

    @Test
    public void testOnEventConsumer(){
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        eventProcessor.onEvent(event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);

        then(logger).should(times(1)).info("1");
        assertThat(consumed).isEqualTo(true);
    }

    @Test
    public void testRegisterConsumer() throws InterruptedException {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        eventProcessor.registerConsumer(Integer.class, event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);

        then(logger).should(times(1)).info("1");
        assertThat(consumed).isEqualTo(true);
    }

    @Test
    public void testOnEventAndRegisterConsumer() throws InterruptedException {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        eventProcessor.registerConsumer(Integer.class, event -> logger.info(event.toString()));
        eventProcessor.onEvent(event -> logger.info(event.toString()));

        boolean consumed = eventProcessor.processEvent(1);

        then(logger).should(times(2)).info("1");
        assertThat(consumed).isEqualTo(true);
    }

    @Test
    public void testNoConsumers() throws InterruptedException {
        EventProcessor<Number> eventProcessor = new EventProcessor<>();
        boolean consumed = eventProcessor.processEvent(1);

        assertThat(consumed).isEqualTo(false);
    }



}
