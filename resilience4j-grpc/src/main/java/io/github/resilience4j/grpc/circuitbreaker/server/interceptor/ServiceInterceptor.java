package io.github.resilience4j.grpc.circuitbreaker.server.interceptor;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.server.ServerCallCircuitBreaker;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.function.Predicate;

public class ServiceInterceptor implements ServerInterceptor {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ServiceInterceptor(
            CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static ServiceInterceptor of(CircuitBreaker circuitBreaker){
        return of(circuitBreaker, Status::isOk);
    }

    public static ServiceInterceptor of(
            CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate){
        return new ServiceInterceptor(circuitBreaker, successStatusPredicate);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {


        ServerCall<ReqT, RespT> callToExecute = ServerCallCircuitBreaker.decorate(
                call, circuitBreaker, successStatusPredicate);

        return next.startCall(callToExecute, headers);
    }
}