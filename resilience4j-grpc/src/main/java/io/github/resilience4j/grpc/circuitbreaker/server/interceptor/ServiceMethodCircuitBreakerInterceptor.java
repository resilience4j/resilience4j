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
package io.github.resilience4j.grpc.circuitbreaker.server.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.server.ServerCallCircuitBreaker;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.function.Predicate;

public class ServiceMethodCircuitBreakerInterceptor implements ServerInterceptor {

    private final MethodDescriptor<?, ?> methodDescriptor;
    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ServiceMethodCircuitBreakerInterceptor(
        MethodDescriptor<?, ?> methodDescriptor,
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        this.methodDescriptor = methodDescriptor;
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static ServiceMethodCircuitBreakerInterceptor from(
        MethodDescriptor<?, ?> methodDescriptor,
        CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        return new ServiceMethodCircuitBreakerInterceptor(methodDescriptor, circuitBreaker, successStatusPredicate);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        ServerCall<ReqT, RespT> callToExecute = call;

        if (call.getMethodDescriptor().getFullMethodName()
            .equals(methodDescriptor.getFullMethodName())) {
            callToExecute = ServerCallCircuitBreaker.decorate(call, circuitBreaker, successStatusPredicate);
        }

        return next.startCall(callToExecute, headers);
    }
}