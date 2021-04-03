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

    /**Zoom
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
                @Override
                public void onResponse(Call call, Response response) {
                    if (retry.context().onResult(response) && ++retryCount < retry.getRetryConfig().getMaxAttempts()) {
                        executableCall().enqueue(retriedCallback(callback));
                    } else {
                        callback.onResponse(call, response);
                    }
                }

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

                private final Exception asException(Throwable throwable) {
                    return throwable instanceof Exception ?
                        (Exception) throwable :
                        new RuntimeException("Throwable", throwable);
                }
            };
        }

        @Override
        public void enqueue(final Callback callback) {
            call.enqueue(retriedCallback(callback));
        }

        @Override
        public Response<T> execute() {
            Response<T> response = null;
            try {
                response = retry.executeCallable(() -> executableCall().execute());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        private Call<T> executableCall() {
            if (call.isExecuted())
                return call.clone();
            return call;
        }

        @Override
        public Call<T> clone() {
            return (new RetryCall<>(executableCall(), retry));
        }

        @Override
        public boolean isExecuted() {
            return call.isExecuted();
        }

        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public Request request() {
            return call.request();
        }
    }
}
