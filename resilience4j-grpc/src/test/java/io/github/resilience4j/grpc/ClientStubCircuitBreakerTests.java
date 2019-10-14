package io.github.resilience4j.grpc;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.grpc.circuitbreaker.CircuitBreakerCallOptions;
import io.github.resilience4j.grpc.circuitbreaker.client.interceptor.ClientCircuitBreakerInterceptor;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleResponse;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientStubCircuitBreakerTests {

    @Rule
    public GrpcServerRule serverRule = new GrpcServerRule();

    private Channel getInterceptedChannel(){
        return ClientInterceptors.intercept(serverRule.getChannel(), new ClientCircuitBreakerInterceptor());
    }

    @Test
    public void decorateSuccessfulCall(){
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");

        serverRule.getServiceRegistry().addService(new SimpleServiceGrpc.SimpleServiceImplBase() {

            @Override
            public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                responseObserver.onNext(SimpleResponse.newBuilder()
                        .setResponseMessage("response: "+request.getRequestMessage())
                        .build());
                responseObserver.onCompleted();
            }
        });

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
                .newBlockingStub(getInterceptedChannel())
                .withOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER, circuitBreaker)
                .withOption(CircuitBreakerCallOptions.SUCCESS_STATUS,
                        status -> status.getCode() != Status.INVALID_ARGUMENT.getCode());

        SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder()
                .setRequestMessage("req:1")
                .build());

        assertThat(response.getResponseMessage()).isEqualTo("response: req:1");
        // No metrics should exist because circuit breaker wasn't used
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);

    }

    @Test
    public void decorateFailedCall(){
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testCircuitBreaker");

        serverRule.getServiceRegistry().addService(new SimpleServiceGrpc.SimpleServiceImplBase() {

            @Override
            public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
                responseObserver.onError(Status.INVALID_ARGUMENT.asRuntimeException());
            }
        });

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc
                .newBlockingStub(getInterceptedChannel())
                .withOption(CircuitBreakerCallOptions.CIRCUIT_BREAKER, circuitBreaker)
                .withOption(CircuitBreakerCallOptions.SUCCESS_STATUS,
                        status -> status.getCode() != Status.INVALID_ARGUMENT.getCode());

        try {
            //noinspection ResultOfMethodCallIgnored
            stub.unaryRpc(SimpleRequest.newBuilder()
                    .setRequestMessage("req:1")
                    .build());
        }catch (StatusRuntimeException ignored){

        }

        // No metrics should exist because circuit breaker wasn't used
        final CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isEqualTo(0);
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);

    }

    public void recordFaliureOnNotPermittedCalls(){

    }

    public void shouldNotRecordFaliureWhenCallCancelled(){

    }

}
