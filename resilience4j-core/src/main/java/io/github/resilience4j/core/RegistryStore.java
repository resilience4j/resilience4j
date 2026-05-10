/*
 * Copyright 2020 KrnSaurabh
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

package io.github.resilience4j.core;

import io.github.resilience4j.core.lang.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public interface RegistryStore<E> {

    E computeIfAbsent(String key,
        Function<? super String, ? extends E> mappingFunction);

    @Nullable E putIfAbsent(String key, E value);

    Optional<E> find(String key);

    Optional<E> remove(String name);

    Optional<E> replace(String name, E newEntry);

    Collection<E> values();

}
