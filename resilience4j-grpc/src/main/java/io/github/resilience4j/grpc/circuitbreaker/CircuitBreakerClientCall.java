package io.github.resilience4j.grpc.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.lang.Nullable;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class CircuitBreakerClientCall<ReqT, RespT>
        extends ClientInterceptors.CheckedForwardingClientCall<ReqT, RespT> {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Status> successStatusPredicate;
    private boolean isCancelled = false;

    CircuitBreakerClientCall(
            ClientCall<ReqT, RespT> delegate,
            CircuitBreaker circuitBreaker,
            Predicate<Status> successStatusPredicate) {

        super(delegate);
        this.circuitBreaker = circuitBreaker;
        this.successStatusPredicate = successStatusPredicate;
    }

    @Override
    protected void checkedStart(
            Listener<RespT> responseListener, Metadata headers) throws Exception {

        if(circuitBreaker != null){
            circuitBreaker.acquirePermission();
            delegate().start(new CircuitBreakerListener(responseListener), headers);
        } else {
            delegate().start(responseListener, headers);
        }
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
        isCancelled = true;
        super.cancel(message, cause);
    }

    private class CircuitBreakerListener extends ForwardingClientCallListener<RespT> {

        private final ClientCall.Listener<RespT> _delegate;
        private final long startTime = System.nanoTime();

        CircuitBreakerListener(Listener<RespT> delegate) {
            this._delegate = delegate;
        }

        @Override
        protected ClientCall.Listener<RespT> delegate() {
            return this._delegate;
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
