/*
 *
 *  Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.event.AdaptiveBulkheadEvent;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;


public class AdaptiveBulkheadEventPublisherTest {

	private Logger logger;
	private AdaptiveBulkhead adaptiveBulkhead;

	@Before
	public void setUp() {
		logger = mock(Logger.class);
		adaptiveBulkhead = AdaptiveBulkhead.ofDefaults("testName");
	}

	@Test
	public void shouldReturnTheSameConsumer() {
		AdaptiveBulkhead.AdaptiveEventPublisher eventPublisher = adaptiveBulkhead.getEventPublisher();
		AdaptiveBulkhead.AdaptiveEventPublisher eventPublisher2 = adaptiveBulkhead.getEventPublisher();

		assertThat(eventPublisher).isEqualTo(eventPublisher2);
	}


	@Test
	public void shouldConsumeOnSuccessEvent() {
		adaptiveBulkhead.getEventPublisher()
				.onSuccess(this::logEventType);

		adaptiveBulkhead.onSuccess(1000, TimeUnit.NANOSECONDS);

		then(logger).should(times(1)).info("SUCCESS");
	}

	@Test
	public void shouldConsumeOnErrorEvent() {
		adaptiveBulkhead.getEventPublisher()
				.onError(this::logEventType);

		adaptiveBulkhead.onError(1000, TimeUnit.NANOSECONDS, new IOException("BAM!"));

		then(logger).should(times(1)).info("ERROR");
	}


	private void logEventType(AdaptiveBulkheadEvent event) {
		logger.info(event.getEventType().toString());
	}

    @Test
    public void shouldConsumeIgnoredErrorEvent() {
      AdaptiveBulkheadConfig adaptiveBulkheadConfig = AdaptiveBulkheadConfig.custom()
				.ignoreExceptions(IOException.class)
				.build();

		adaptiveBulkhead = AdaptiveBulkhead.of("test", adaptiveBulkheadConfig);

		adaptiveBulkhead.getEventPublisher()
				.onIgnoredError(this::logEventType);

		adaptiveBulkhead.onError(10000, TimeUnit.NANOSECONDS, new IOException("BAM!"));

		then(logger).should(times(1)).info("IGNORED_ERROR");
	}

}
