/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.cache.event;

/**
 * A CacheEvent which informs that accessing the cache has caused an exception.
 */
public class CacheOnErrorEvent extends AbstractCacheEvent {

    private final Throwable throwable;

    public CacheOnErrorEvent(String cacheName, Throwable throwable) {
        super(cacheName);
        this.throwable = throwable;
    }

    @Override
    public Type getEventType() {
        return Type.ERROR;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        return String.format("%s: Cache '%s' recorded an error: '%s'.",
            getCreationTime(),
            getCacheName(),
            getThrowable().toString());
    }
}
