/*
 *  Copyright 2019 Marco Ferrer
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
 */
package io.github.resilience4j.grpc.circuitbreaker.server;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.server.interceptor.ServiceInterceptor;
import io.github.resilience4j.grpc.circuitbreaker.server.interceptor.ServiceMethodInterceptor;
import io.grpc.MethodDescriptor;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class ServerCircuitBreakerDecorator<T extends ServerCircuitBreakerDecorator<T>> {

    protected final List<ServerInterceptor> interceptors = new ArrayList<>();

    public T withCircuitBreakerForMethod(
            MethodDescriptor<?, ?> methodDescriptor, CircuitBreaker circuitBreaker) {
        return withCircuitBreakerForMethod(methodDescriptor, circuitBreaker, Status::isOk);
    }

    public T withCircuitBreakerForMethod(
            MethodDescriptor<?, ?> methodDescriptor,
            CircuitBreaker circuitBreaker,
            Predicate<Status> successStatusPredicate) {
        interceptors.add(ServiceMethodInterceptor.of(methodDescriptor, circuitBreaker, successStatusPredicate));
        return thisT();
    }

    public T withCircuitBreaker(CircuitBreaker circuitBreaker) {
        return withCircuitBreaker(circuitBreaker, Status::isOk);
    }

    public T withCircuitBreaker(
            CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {
        interceptors.add(ServiceInterceptor.of(circuitBreaker, successStatusPredicate));
        return thisT();
    }

    @SuppressWarnings("unchecked")
    private T thisT(){
        return (T) this;
    }

    abstract public ServerServiceDefinition build();
}
