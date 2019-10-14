package io.github.resilience4j.grpc.circuitbreaker.client.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.client.ClientCallCircuitBreaker;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import java.util.function.Predicate;

public class ChannelCircuitBreakerInterceptor implements ClientInterceptor {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ChannelCircuitBreakerInterceptor(
            CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static ChannelCircuitBreakerInterceptor of(CircuitBreaker circuitBreaker){
        return new ChannelCircuitBreakerInterceptor(circuitBreaker, Status::isOk);
    }

    public static ChannelCircuitBreakerInterceptor of(
            CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate){
        return new ChannelCircuitBreakerInterceptor(circuitBreaker, successStatusPredicate);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return ClientCallCircuitBreaker.decorate(
                next.newCall(method, callOptions), circuitBreaker, successStatusPredicate);
    }
}
