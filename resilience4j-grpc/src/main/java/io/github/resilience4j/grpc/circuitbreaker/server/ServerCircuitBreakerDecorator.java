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
