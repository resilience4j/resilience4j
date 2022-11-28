/*
 * Copyright 2019 Kyuhyen Hwang
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
package io.github.resilience4j.fallback;

import io.github.resilience4j.core.functions.CheckedSupplier;

import java.util.List;

/**
 * {@link FallbackDecorator} resolver
 */
public class FallbackDecorators {

    private final List<FallbackDecorator> fallbackDecorators;
    private final FallbackDecorator defaultFallbackDecorator = new DefaultFallbackDecorator();

    public FallbackDecorators(List<FallbackDecorator> fallbackDecorators) {
        this.fallbackDecorators = fallbackDecorators;
    }

    /**
     * find a {@link FallbackDecorator} by return type of the {@link FallbackMethod} and decorate
     * supplier
     *
     * @param fallbackMethod fallback method that handles supplier's exception
     * @param supplier       original function
     * @return a function which is decorated by a {@link FallbackMethod}
     */
    public CheckedSupplier<Object> decorate(FallbackMethod fallbackMethod,
                                            CheckedSupplier<Object> supplier) {
        return get(fallbackMethod.getReturnType())
            .decorate(fallbackMethod, supplier);
    }

    private FallbackDecorator get(Class<?> returnType) {
        return fallbackDecorators.stream().filter(it -> it.supports(returnType))
            .findFirst()
            .orElse(defaultFallbackDecorator);
    }

    public List<FallbackDecorator> getFallbackDecorators() {
        return fallbackDecorators;
    }
}
