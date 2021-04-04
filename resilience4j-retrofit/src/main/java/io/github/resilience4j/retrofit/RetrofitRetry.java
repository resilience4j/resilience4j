/*
 *
 * Copyright 2021 Ipuvi Mishra
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package io.github.resilience4j.retrofit;

import io.github.resilience4j.retrofit.internal.DecoratedCall;
import io.github.resilience4j.retry.Retry;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Decorates a Retrofit {@link Call} to inform a Retry when an retryOnResults matches and retryExceptions are thrown.
 * All exceptions are marked as errors or responses not matching the supplied predicate.
 * For example:
 * <p>
 * <code>
 * RetrofitRetry.decorateCall(retry, call);
 * </code>
 */
public interface RetrofitRetry {

    /**
     * Decorate {@link Call}s allow Retry functionality.
     *
     * @param retry Retry to apply
     * @param call  Call to decorate
     * @return Original Call decorated with Retry
     */
    static <T> Call<T> decorateCall(final Retry retry, final Call<T> call) {
        return new RetryCall<>(call, retry);
    }

    class RetryCall<T> extends DecoratedCall<T> {

        private final Call<T> call;
        private final Retry retry;
        private int retryCount = 0;

        public RetryCall(Call<T> call, Retry retry) {
            super(call);
            this.call = call;
            this.retry = retry;
        }

        private final Callback retriedCallback(Callback callback) {
            return new Callback() {

                /**
                 * Invoked for a received HTTP response.
                 */
                @Override
                public void onResponse(Call call, Response response) {
                    if (retry.context().onResult(response) && ++retryCount < retry.getRetryConfig().getMaxAttempts()) {
                        executableCall().enqueue(retriedCallback(callback));
                    } else {
                        callback.onResponse(call, response);
                    }
                }

                /**
                 * Invoked when a network exception occurred talking to the server or when an unexpected
                 * exception occurred creating the request or processing the response.
                 */
                @Override
                public void onFailure(Call call, Throwable throwable) {
                    if (retry.getRetryConfig().getExceptionPredicate().test(throwable) && ++retryCount < retry.getRetryConfig().getMaxAttempts()) {
                        try {
                            retry.context().onError(asException(throwable));
                            executableCall().enqueue(retriedCallback(callback));
                        } catch (Throwable throwable1) {
                            callback.onFailure(call, throwable);
                        }
                    } else {
                        callback.onFailure(call, throwable);
                    }
                }

                /**
                 * resilience4j accepts exceptions only
                 */
                private final Exception asException(Throwable throwable) {
                    return throwable instanceof Exception ?
                        (Exception) throwable :
                        new RuntimeException("Throwable", throwable);
                }
            };
        }

        /**
         * Asynchronously send the request and notify callback of its response or if an error
         * occurred talking to the server, creating the request, or processing the response.
         */
        @Override
        public void enqueue(final Callback callback) {
            call.enqueue(retriedCallback(callback));
        }

        /**
         * Synchronously send the request and return its response
         */
        @Override
        public Response<T> execute() {
            Response<T> response = null;

            try {
                response = retry.executeCallable(() -> executableCall().execute());
            } catch (Exception e) {
                throw new RuntimeException();
            }
            return response;
        }

        /**
         * Returns cloned call, if the call has been either {@linkplain #execute() executed} or {@linkplain
         * #enqueue(Callback) enqueued} already else would return the actual call.
         */
        private Call<T> executableCall() {
            if (call.isExecuted())
                return call.clone();
            return call;
        }

        /**
         * Create a new, identical call to this one which can be enqueued or executed even if this call
         * has already been.
         */
        @Override
        public Call<T> clone() {
            return (new RetryCall<>(executableCall(), retry));
        }

        /**
         * Returns true if this call has been either {@linkplain #execute() executed} or {@linkplain
         * #enqueue(Callback) enqueued}. It is an error to execute or enqueue a call more than once.
         */
        @Override
        public boolean isExecuted() {
            return call.isExecuted();
        }

        /**
         * True if {@link #cancel()} was called.
         */
        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }

        /**
         * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
         * yet been executed it never will be.
         */
        @Override
        public void cancel() {
            call.cancel();
        }

        /**
         * The original HTTP request.
         */
        @Override
        public Request request() {
            return call.request();
        }
    }
}
