package io.github.resilience4j.grpc.circuitbreaker.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ClientCallCircuitBreaker<ReqT, RespT>
        extends ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT> {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;
    private boolean isCancelled = false;

    protected ClientCallCircuitBreaker(
            ClientCall<ReqT, RespT> delegate,
            CircuitBreaker circuitBreaker,
            Predicate<Status> successStatusPredicate) {

        super(delegate);
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    public static <ReqT, RespT> ClientCallCircuitBreaker<ReqT, RespT> decorate(
            ClientCall<ReqT, RespT> call, CircuitBreaker circuitBreaker){
        return new ClientCallCircuitBreaker<>(call, circuitBreaker, Status::isOk);
    }

    public static <ReqT, RespT> ClientCallCircuitBreaker<ReqT, RespT> decorate(
            ClientCall<ReqT, RespT> call, CircuitBreaker circuitBreaker, Predicate<Status> successStatusPredicate){
        return new ClientCallCircuitBreaker<>(call, circuitBreaker, successStatusPredicate);
    }

    @Override
    protected void checkedStart(
            Listener<RespT> responseListener, Metadata headers) throws Exception {

        circuitBreaker.acquirePermission();
        delegate().start(new CircuitBreakerListener(responseListener), headers);
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
        isCancelled = true;
        super.cancel(message, cause);
    }

    private class CircuitBreakerListener
            extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT> {

        private final long startTime = System.nanoTime();

        CircuitBreakerListener(Listener<RespT> delegate) {
            super(delegate);
        }

        @Override
        public void onClose(Status status, Metadata trailers) {

            if(isCancelled){
                circuitBreaker.releasePermission();
            }else if(successStatusPredicate.test(status)){
                circuitBreaker.onSuccess(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }else{
                circuitBreaker.onError(
                        System.nanoTime() - startTime, TimeUnit.NANOSECONDS,
                        status.asRuntimeException(trailers));
            }
            super.onClose(status, trailers);
        }
    }

}
