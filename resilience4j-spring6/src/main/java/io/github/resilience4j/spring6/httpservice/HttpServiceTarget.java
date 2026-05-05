/*
 * Copyright 2026 Bobae Kim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.spring6.httpservice;

import java.util.Objects;

/**
 * Holds metadata about the HTTP Service target being proxied.
 *
 * @param <T> the type of the HTTP Service
 */
public final class HttpServiceTarget<T> {

    private final Class<T> type;
    private final String name;

    private HttpServiceTarget(Class<T> type, String name) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a new target with the interface type and uses the simple class name as the name.
     *
     * @param type the HTTP Service class
     * @param <T>  the type of the interface
     * @return a new HttpServiceTarget
     */
    public static <T> HttpServiceTarget<T> of(Class<T> type) {
        return new HttpServiceTarget<>(type, type.getSimpleName());
    }

    /**
     * Creates a new target with the interface type and a custom name.
     *
     * @param type the HTTP Service class
     * @param name the name for this target (used in metrics/logging)
     * @param <T>  the type of the interface
     * @return a new HttpServiceTarget
     */
    public static <T> HttpServiceTarget<T> of(Class<T> type, String name) {
        return new HttpServiceTarget<>(type, name);
    }

    /**
     * @return the HTTP Service class
     */
    public Class<T> type() {
        return type;
    }

    /**
     * @return the name of this target
     */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpServiceTarget<?> that)) return false;
        return type.equals(that.type) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return "HttpServiceTarget{type=" + type.getName() + ", name='" + name + "'}";
    }
}
