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
 *  See the License for the speci`fic language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead.event;

import java.time.ZonedDateTime;
import java.util.Map;

public abstract class AbstractBulkheadLimitEvent implements AdaptiveBulkheadEvent {

	private final String bulkheadName;
	// TODO replace by fields
	private final Map<String, String> eventData;
	private final ZonedDateTime creationTime;

    AbstractBulkheadLimitEvent(String bulkheadName, Map<String, String> eventData) {
		this.bulkheadName = bulkheadName;
		this.eventData = eventData;
		this.creationTime = ZonedDateTime.now();
	}

	@Override
	public String getBulkheadName() {
		return bulkheadName;
	}

	@Override
	public Map<String, String> eventData() {
		return eventData;
	}

	@Override
	public ZonedDateTime getCreationTime() {
		return creationTime;
	}
}
