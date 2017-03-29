package io.github.resilience4j.retrofit;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * Creates a Retrofit {@link CallAdapter.Factory} that decorates a Call to provide integration with a
 * {@link CircuitBreaker} using {@link RetrofitCircuitBreaker}
 */
public final class CircuitBreakerCallAdapter extends CallAdapter.Factory {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Response> successResponse;

    /**
     * Create a circuit-breaking call adapter that decorates retrofit calls
     * @param circuitBreaker circuit breaker to use
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static CircuitBreakerCallAdapter of(final CircuitBreaker circuitBreaker) {
        return new CircuitBreakerCallAdapter(circuitBreaker);
    }

    /**
     * Create a circuit-breaking call adapter that decorates retrofit calls
     * @param circuitBreaker circuit breaker to use
     * @param successResponse {@link Predicate} that determines whether the {@link Call} {@link Response} should be considered successful
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static CircuitBreakerCallAdapter of(final CircuitBreaker circuitBreaker, final Predicate<Response> successResponse) {
        return new CircuitBreakerCallAdapter(circuitBreaker, successResponse);
    }

    private CircuitBreakerCallAdapter(final CircuitBreaker circuitBreaker) {
        this(circuitBreaker, Response::isSuccessful);
    }

    private CircuitBreakerCallAdapter(final CircuitBreaker circuitBreaker, final Predicate<Response> successResponse) {
        this.circuitBreaker = circuitBreaker;
        this.successResponse = successResponse;
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Call.class) {
            return null;
        }

        final Type responseType = getCallResponseType(returnType);
        return new CallAdapter<Call<?>>() {
            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public <R> Call<R> adapt(Call<R> call) {
                return RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, successResponse);
            }
        };
    }

    private static Type getCallResponseType(Type returnType) {
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>");
        }
        return getParameterUpperBound(0, (ParameterizedType) returnType);
    }

}