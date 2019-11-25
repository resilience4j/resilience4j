package io.github.resilience4j.retrofit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class EnqueueDecorator {

    public static <T> Response<T> enqueue(Call<T> call) throws Throwable {
        final CountDownLatch enqueueLatch = new CountDownLatch(1);
        final AtomicReference<Response<T>> responseReference = new AtomicReference<>();
        final AtomicReference<Throwable> failureReference = new AtomicReference<>();
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(final Call<T> call, final Response<T> response) {
                responseReference.set(response);
                enqueueLatch.countDown();
            }

            @Override
            public void onFailure(final Call<T> call, final Throwable t) {
                failureReference.set(t);
                enqueueLatch.countDown();
            }
        });

        enqueueLatch.await();
        if (failureReference.get() != null) {
            throw failureReference.get();
        }
        return responseReference.get();
    }

    public static void performCatchingEnqueue(Call<?> call) {
        try {
            enqueue(call);
        } catch (Throwable ignored) {
        }
    }
}
