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

import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Main class for combining Spring HTTP Service clients with Resilience4j.
 *
 * <p>Usage:
 * <pre>{@code
 * RestClient restClient = RestClient.builder()
 *     .baseUrl("http://localhost:8080")
 *     .build();
 *
 * HttpServiceProxyFactory factory = HttpServiceProxyFactory
 *     .builderFor(RestClientAdapter.create(restClient))
 *     .build();
 *
 * HttpServiceDecorators decorators = HttpServiceDecorators.builder()
 *     .withCircuitBreaker(circuitBreaker)
 *     .withRetry(retry)
 *     .withFallback(fallbackInstance)
 *     .build();
 *
 * MyService service = Resilience4jHttpService.builder(decorators)
 *     .factory(factory)
 *     .build(MyService.class);
 * }</pre>
 *
 * @see HttpServiceDecorators
 * @see HttpServiceDecorator
 */
public final class Resilience4jHttpService {

    private Resilience4jHttpService() {
    }

    /**
     * Creates a new builder with the specified decorator.
     *
     * @param decorator the decorator to apply to HTTP Service method invocations
     * @return a new Builder instance
     */
    public static Builder builder(HttpServiceDecorator decorator) {
        return new Builder(decorator);
    }

    /**
     * Builder for creating resilient HTTP Service client proxies.
     */
    public static final class Builder {

        private final HttpServiceDecorator decorator;
        private HttpServiceProxyFactory factory;

        Builder(HttpServiceDecorator decorator) {
            this.decorator = Objects.requireNonNull(decorator, "decorator must not be null");
        }

        /**
         * Configure with a HttpServiceProxyFactory.
         *
         * @param factory the factory to use for creating HTTP Service clients
         * @return this builder
         */
        public Builder factory(HttpServiceProxyFactory factory) {
            this.factory = Objects.requireNonNull(factory, "factory must not be null");
            return this;
        }

        /**
         * Build a resilient HTTP Service client proxy.
         *
         * @param serviceType the HTTP Service class
         * @param <T>         the type of the service interface
         * @return a decorated proxy implementing the service interface
         */
        public <T> T build(Class<T> serviceType) {
            return build(serviceType, serviceType.getSimpleName());
        }

        /**
         * Build a resilient HTTP Service client proxy with a custom name.
         *
         * @param serviceType the HTTP Service class
         * @param name        the name for this client (used in metrics/logging)
         * @param <T>         the type of the service interface
         * @return a decorated proxy implementing the service interface
         */
        @SuppressWarnings("unchecked")
        public <T> T build(Class<T> serviceType, String name) {
            Objects.requireNonNull(serviceType, "serviceType must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(factory, "factory must not be null");

            if (!serviceType.isInterface()) {
                throw new IllegalArgumentException("serviceType must be an interface");
            }

            InvocationHandler invocationHandler = new DecoratorInvocationHandler(
                    HttpServiceTarget.of(serviceType, name),
                    factory.createClient(serviceType),
                    decorator
            );

            return (T) Proxy.newProxyInstance(
                    serviceType.getClassLoader(),
                    new Class<?>[]{serviceType},
                    invocationHandler
            );
        }
    }
}
