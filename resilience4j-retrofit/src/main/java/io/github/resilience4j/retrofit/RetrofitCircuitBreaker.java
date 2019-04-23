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
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.retrofit.internal.DecoratedCall;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Decorates a Retrofit {@link Call} to inform a {@link CircuitBreaker} when an exception is thrown.
 * All exceptions are marked as errors or responses not matching the supplied predicate.  For example:
 * <p>
 * <code>
 * RetrofitCircuitBreaker.decorateCall(circuitBreaker, call, Response::isSuccessful);
 * </code>
 */
public interface RetrofitCircuitBreaker {

    /**
     * Decorate {@link Call}s allow {@link CircuitBreaker} functionality.
     *
     * @param circuitBreaker  {@link CircuitBreaker} to apply
     * @param call            Call to decorate
     * @param responseSuccess determines whether the response should be considered an expected response
     * @param <T> Response type of call
     * @return Original Call decorated with CircuitBreaker
     */
    static <T> Call<T> decorateCall(final CircuitBreaker circuitBreaker, final Call<T> call, final Predicate<Response> responseSuccess) {
        return new DecoratedCall<T>(call) {

            @Override
            public void enqueue(final Callback<T> callback) {
                try {
                    CircuitBreakerUtils.isCallPermitted(circuitBreaker);
                } catch (CircuitBreakerOpenException cb) {
                    callback.onFailure(call, cb);
                    return;
                }

                final StopWatch stopWatch = StopWatch.start();
                call.enqueue(new Callback<T>() {
                    @Override
                    public void onResponse(final Call<T> call, final Response<T> response) {
                        if (responseSuccess.test(response)) {
                            circuitBreaker.onSuccess(stopWatch.stop().toNanos());
                        } else {
                            final Throwable throwable = new Throwable("Response error: HTTP " + response.code() + " - " + response.message());
                            circuitBreaker.onError(stopWatch.stop().toNanos(), throwable);
                        }
                        callback.onResponse(call, response);
                    }

                    @Override
                    public void onFailure(final Call<T> call, final Throwable t) {
                        circuitBreaker.onError(stopWatch.stop().toNanos(), t);
                        callback.onFailure(call, t);
                    }
                });
            }

            @Override
            public Response<T> execute() throws IOException {
                CircuitBreakerUtils.isCallPermitted(circuitBreaker);
                final StopWatch stopWatch = StopWatch.start();
                try {
                    final Response<T> response = call.execute();

                    if (responseSuccess.test(response)) {
                        circuitBreaker.onSuccess(stopWatch.stop().toNanos());
                    } else {
                        final Throwable throwable = new Throwable("Response error: HTTP " + response.code() + " - " + response.message());
                        circuitBreaker.onError(stopWatch.stop().toNanos(), throwable);
                    }

                    return response;
                } catch (Throwable throwable) {
                    circuitBreaker.onError(stopWatch.stop().toNanos(), throwable);
                    throw throwable;
                }
            }
        };
    }

}