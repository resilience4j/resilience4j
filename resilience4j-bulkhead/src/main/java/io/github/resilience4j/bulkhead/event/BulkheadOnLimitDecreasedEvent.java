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
package io.github.resilience4j.bulkhead.event;

import java.util.Map;

/**
 * A BulkheadEvent which informs that a limit has been decreased
 */
public class BulkheadOnLimitDecreasedEvent extends AbstractBulkheadLimitEvent {

	public BulkheadOnLimitDecreasedEvent(String bulkheadName, Map<String, String> eventData) {
		super(bulkheadName, eventData);
	}

	@Override
	public Type getEventType() {
		return Type.LIMIT_DECREASED;
	}

    public int getNewMaxConcurrentCalls(){
        return Integer.parseInt(eventData().get("newMaxConcurrentCalls"));
    }

	@Override
	public String toString() {
		return String.format(
				"%s: Bulkhead '%s' limit decreased.",
				eventData(),
				getBulkheadName()
		);
	}
}
