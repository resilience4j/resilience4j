package io.github.resilience4j.grpc.circuitbreaker.server;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ServerCallCircuitBreaker<ReqT, RespT>
        extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>{

    private final long startTime = System.nanoTime();
    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;

    protected ServerCallCircuitBreaker(
            ServerCall<ReqT, RespT> delegate, CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate) {
        super(delegate);
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static <ReqT, RespT> ServerCallCircuitBreaker<ReqT, RespT> decorate(
            ServerCall<ReqT, RespT> call, CircuitBreaker circuitBreaker){
        return new ServerCallCircuitBreaker<>(call, circuitBreaker, Status::isOk);
    }

    public static <ReqT, RespT> ServerCallCircuitBreaker<ReqT, RespT> decorate(
            ServerCall<ReqT, RespT> call, CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate){
        return new ServerCallCircuitBreaker<>(call, circuitBreaker, successStatusPredicate);
    }

    @Override
    public void close(Status status, Metadata trailers) {
        if(delegate().isCancelled()){
            circuitBreaker.releasePermission();
        }else if(successStatusPredicate.test(status)){
            circuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }else{
            circuitBreaker.onError(
                    System.nanoTime() - startTime, TimeUnit.NANOSECONDS,
                    status.asRuntimeException(trailers));
        }
        super.close(status, trailers);
    }
}
