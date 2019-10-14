package io.github.resilience4j.grpc.circuitbreaker.client.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.CircuitBreakerCallOptions;
import io.github.resilience4j.grpc.circuitbreaker.client.ClientCallCircuitBreaker;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

public class ClientCircuitBreakerInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        CircuitBreaker circuitBreaker = callOptions
                .getOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER);

        if(circuitBreaker != null){
            return ClientCallCircuitBreaker.decorate(
                    next.newCall(method, callOptions), circuitBreaker,
                    callOptions.getOption(CircuitBreakerCallOptions.SUCCESS_STATUS));
        } else {
            return next.newCall(method, callOptions);
        }
    }
}
