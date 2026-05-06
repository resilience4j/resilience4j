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

/**
 * A {@link BulkheadFullException} signals that the bulkhead is full.
 */
public class BulkheadFullException extends RuntimeException {
    private final String bulkheadName;

    private BulkheadFullException(String bulkheadName, String message, boolean writableStackTrace) {
        super(message, null, false, writableStackTrace);

        this.bulkheadName = bulkheadName;
    }

    private BulkheadFullException(String bulkheadName, boolean writableStackTrace) {
        super(String.format("Bulkhead '%s' is full and does not permit further calls", bulkheadName), null, false, writableStackTrace);

        this.bulkheadName = bulkheadName;
    }

    /**
     * Static method to construct a {@link BulkheadFullException} with a Bulkhead.
     *
     * @param bulkhead the Bulkhead.
     */
    public static BulkheadFullException createBulkheadFullException(Bulkhead bulkhead) {
        boolean writableStackTraceEnabled = bulkhead.getBulkheadConfig()
            .isWritableStackTraceEnabled();

        String bulkheadName = bulkhead.getName();

        if (Thread.currentThread().isInterrupted()) {
            return new BulkheadFullAndInterruptedException(bulkheadName, writableStackTraceEnabled);
        } else {
            return new BulkheadFullException(bulkheadName, writableStackTraceEnabled);
        }
    }

    /**
     * Static method to construct a {@link BulkheadFullException} with a ThreadPoolBulkhead.
     *
     * @param bulkhead the Bulkhead.
     */
    public static BulkheadFullException createBulkheadFullException(ThreadPoolBulkhead bulkhead) {
        boolean writableStackTraceEnabled = bulkhead.getBulkheadConfig()
            .isWritableStackTraceEnabled();

        String bulkheadName = bulkhead.getName();

        return new BulkheadFullException(bulkheadName, writableStackTraceEnabled);
    }

    /**
     * Static method to construct a {@link BulkheadFullException} with a Bulkhead.
     *
     * @param bulkheadName the name of the bulkhead
     * @param enableWritableStackTrace whether to enable the writable stack trace.
     */
    public static BulkheadFullException createBulkheadFullException(String bulkheadName, boolean enableWritableStackTrace) {
        return new BulkheadFullException(bulkheadName, enableWritableStackTrace);
    }

    public String getBulkheadName() {
        return bulkheadName;
    }

    /**
     *  * A {@link BulkheadFullAndInterruptedException} signals that the bulkhead is full and the thread was interrupted during permission wait.
     */
    public static class BulkheadFullAndInterruptedException extends BulkheadFullException {
        private BulkheadFullAndInterruptedException(String bulkheadName, boolean writableStackTrace) {
            super(bulkheadName, String.format("Bulkhead '%s' is full and thread was interrupted during permission wait", bulkheadName), writableStackTrace);
        }
    }
}
