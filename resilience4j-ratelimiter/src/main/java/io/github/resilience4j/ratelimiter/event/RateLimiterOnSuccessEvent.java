/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.event;

public class RateLimiterOnSuccessEvent extends AbstractRateLimiterEvent {
    public RateLimiterOnSuccessEvent(String rateLimiterName) {
        super(rateLimiterName, 1);
    }
	
    public RateLimiterOnSuccessEvent(String rateLimiterName, int numberOfPermits) {
        super(rateLimiterName, numberOfPermits);
    }

    @Override
    public Type getEventType() {
        return Type.SUCCESSFUL_ACQUIRE;
    }
}
