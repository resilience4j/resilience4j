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
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;

/**
 * A {@link BulkheadFullException} signals that the bulkhead is full.
 */
public class BulkheadFullException extends RuntimeException {

    private static final String ERROR_FURTHER_CALLS = "Bulkhead '%s' is full and does not permit further calls";
    private static final String ERROR_PERMISSION_WAIT = "Bulkhead '%s' is full and thread was interrupted during permission wait";

    private BulkheadFullException(String message, boolean writableStackTrace) {
        super(message, null, false, writableStackTrace);
    }

    /**
     * Static method to construct a {@link BulkheadFullException} with a Bulkhead.
     *
     * @param bulkhead the Bulkhead.
     */
    public static BulkheadFullException createBulkheadFullException(Bulkhead bulkhead) {
	    boolean writableStackTraceEnabled = bulkhead.getBulkheadConfig()
            .isWritableStackTraceEnabled();

        String message;
        if (Thread.currentThread().isInterrupted()) {
            message = String.format(ERROR_PERMISSION_WAIT, bulkhead.getName());
        } else {
            message = String.format(ERROR_FURTHER_CALLS, bulkhead.getName());
        }

        return new BulkheadFullException(message, writableStackTraceEnabled);
    }

    /**
     * Static method to construct a {@link BulkheadFullException} with a ThreadPoolBulkhead.
     *
     * @param bulkhead the Bulkhead.
     */
    public static BulkheadFullException createBulkheadFullException(ThreadPoolBulkhead bulkhead) {
        boolean writableStackTraceEnabled = bulkhead.getBulkheadConfig()
            .isWritableStackTraceEnabled();

        String message = String.format(ERROR_FURTHER_CALLS, bulkhead.getName());

        return new BulkheadFullException(message, writableStackTraceEnabled);
    }
	/**
	 * The constructor with a message.
	 *
	 * @param bulkhead the AdaptiveLimitBulkhead.
	 */
	public BulkheadFullException(AdaptiveBulkhead bulkhead) {
        super(String.format(ERROR_FURTHER_CALLS, bulkhead.getName()));
	}

    /**
     * The constructor with a message.
     *
     * @param bulkhead the AdaptiveBulkheadStateMachine.
     */
    public static BulkheadFullException createBulkheadFullException(AdaptiveBulkhead bulkhead) {
        boolean writableStackTraceEnabled = bulkhead.getBulkheadConfig().isWritableStackTraceEnabled();

        String message = String.format(ERROR_FURTHER_CALLS, bulkhead.getName());

        return new BulkheadFullException(message, writableStackTraceEnabled);
    }
}
