package io.github.resilience4j.retrofit.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import retrofit2.Call;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class DecoratedCallTest {

    private final Call<String> call = mock(StringCall.class);
    private final Call<String> decorated = new DecoratedCall<String>(call) {
        @Override
        public Call<String> clone() {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void passThroughCallsToDecoratedObject() throws IOException {
        decorated.cancel();
        verify(call).cancel();

        decorated.enqueue(null);
        verify(call).enqueue(any());

        decorated.isExecuted();
        verify(call).isExecuted();

        decorated.isCanceled();
        verify(call).isCanceled();

        decorated.request();
        verify(call).request();

        decorated.execute();
        verify(call).execute();
    }

    private interface StringCall extends Call<String> {

    }
}
