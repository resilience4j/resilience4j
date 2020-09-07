/*
 *
 *  Copyright 2020: Robert Winkler
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
package io.github.resilience4j.core.exception;

/**
 * Exception indicating that the permission wasn't acquired because the task was cancelled or thread
 * interrupted.
 * <p>
 * We extend it from IllegalStateException to preserve backwards compatibility with version 1.0.0 of
 * Resilience4j
 */
public class AcquirePermissionCancelledException extends IllegalStateException {

    private static final String DEFAULT_MESSAGE = "Thread was interrupted while waiting for a permission";

    public AcquirePermissionCancelledException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a {@code AcquirePermissionCancelledException} with detail message.
     *
     * @param message the detail message
     */
    public AcquirePermissionCancelledException(String message) {
        super(message);
    }
}
