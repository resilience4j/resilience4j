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

import io.github.resilience4j.ratelimiter.RateLimiter;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Creates a Retrofit {@link CallAdapter.Factory} that decorates a Call to provide integration with
 * a supplied {@link RateLimiter}
 */
public final class RateLimiterCallAdapter extends CallAdapter.Factory {

    private final RateLimiter rateLimiter;

    private RateLimiterCallAdapter(final RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * Create a rate-limiting call adapter factory that decorates retrofit calls
     *
     * @param rateLimiter rate limiter to use
     * @return a {@link CallAdapter.Factory} that can be passed into the {@link Retrofit.Builder}
     */
    public static RateLimiterCallAdapter of(final RateLimiter rateLimiter) {
        return new RateLimiterCallAdapter(rateLimiter);
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
                return nextAdapter.adapt(RetrofitRateLimiter.decorateCall(rateLimiter, call));
            }
        };
    }
}
