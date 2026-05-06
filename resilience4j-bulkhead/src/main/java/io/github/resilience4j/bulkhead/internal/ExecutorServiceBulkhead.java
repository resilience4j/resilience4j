/*
 *
 *  Copyright 2019 Robert Winkler, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.internal;


import io.github.resilience4j.bulkhead.GenericBulkhead;
import io.github.resilience4j.core.lang.NonNull;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.util.Collections.emptyMap;

/**
 * A Bulkhead implementation delegating to an injected ExecutorService.
 */
public class ExecutorServiceBulkhead extends AbstractExecutorServiceBulkhead<ExecutorService> implements GenericBulkhead {

    private final boolean writableStackTraceEnabled;

    /**
     * Creates a bulkhead using given executor service
     *
     * @param name                     the name of this bulkhead
     * @param executor                 the executor service
     * @param enableWritableStackTrace whether to enable writable stack trace
     */
    public ExecutorServiceBulkhead(String name,
                                   @NonNull ExecutorService executor,
                                   boolean enableWritableStackTrace) {
        this(name, emptyMap(), executor, enableWritableStackTrace);
    }

    /**
     * Creates a bulkhead using given executor service
     *
     * @param name                     the name of this bulkhead
     * @param tags                     tags to add to the Bulkhead
     * @param executor                 the executor service
     * @param enableWritableStackTrace whether to enable writable stack trace
     */
    public ExecutorServiceBulkhead(String name,
                                   Map<String, String> tags,
                                   @NonNull ExecutorService executor,
                                   boolean enableWritableStackTrace) {
        super(executor, name, tags);
        this.writableStackTraceEnabled = enableWritableStackTrace;
    }

    @Override
    public String toString() {
        return String.format("ExecutorServiceBulkhead '%s'", this.name);
    }

    @Override
    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }
}
