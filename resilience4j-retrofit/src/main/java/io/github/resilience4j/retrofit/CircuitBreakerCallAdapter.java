/*
 *
 *  Copyright 2017 Christopher Pilsworth
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.retrofit;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * Creates a Retrofit {@link CallAdapter.Factory} that decorates a Call to provide integration with
 * a {@link CircuitBreaker}
 */
public final class CircuitBreakerCallAdapter extends CallAdapter.Factory {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<Response> successResponse;

    private CircuitBreakerCallAdapter(final CircuitBreaker circuitBreaker,
        final Predicate<Response> successResponse) {
        this.circuitBreaker = circuitBreaker;
        this.successResponse = successResponse;
    }

    /**
     * Create a circuit-breaking call adapter that decorates retrofit calls
     *
     * @param circuitBreaker circuit breaker to use
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static CircuitBreakerCallAdapter of(final CircuitBreaker circuitBreaker) {
        return of(circuitBreaker, Response::isSuccessful);
    }

    /**
     * Create a circuit-breaking call adapter that decorates retrofit calls
     *
     * @param circuitBreaker  circuit breaker to use
     * @param successResponse {@link Predicate} that determines whether the {@link Call} {@link
     *                        Response} should be considered successful
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static CircuitBreakerCallAdapter of(final CircuitBreaker circuitBreaker,
        final Predicate<Response> successResponse) {
        return new CircuitBreakerCallAdapter(circuitBreaker, successResponse);
    }

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        @SuppressWarnings("unchecked")
        CallAdapter<Object, Object> nextAdapter = (CallAdapter<Object, Object>) retrofit
            .nextCallAdapter(this, returnType, annotations);

        return new CallAdapter<Object, Object>() {
            @Override
            public Type responseType() {
                return nextAdapter.responseType();
            }

            @Override
            public Object adapt(Call<Object> call) {
                return nextAdapter.adapt(
                    RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, successResponse));
            }
        };
    }
}
