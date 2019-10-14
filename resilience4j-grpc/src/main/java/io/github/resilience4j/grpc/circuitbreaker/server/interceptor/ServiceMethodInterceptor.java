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

public class ServiceMethodInterceptor implements ServerInterceptor {

    private final MethodDescriptor<?,?> methodDescriptor;
    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    private ServiceMethodInterceptor(
            MethodDescriptor<?, ?> methodDescriptor,
            CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {

        this.methodDescriptor = methodDescriptor;
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }


    public static ServiceMethodInterceptor of(MethodDescriptor<?, ?> methodDescriptor, CircuitBreaker circuitBreaker){
        return of(methodDescriptor, circuitBreaker, Status::isOk);
    }

    public static ServiceMethodInterceptor of(MethodDescriptor<?, ?> methodDescriptor,
                                              CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate){
        return new ServiceMethodInterceptor(methodDescriptor, circuitBreaker, successStatusPredicate);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        ServerCall<ReqT, RespT> callToExecute = call;

        if(call.getMethodDescriptor().getFullMethodName()
                .equals(methodDescriptor.getFullMethodName())) {
            callToExecute = ServerCallCircuitBreaker.decorate(call, circuitBreaker, successStatusPredicate);
        }

        return next.startCall(callToExecute, headers);
    }
}