package io.github.resilience4j.retrofit.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import retrofit2.Call;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

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
        Mockito.verify(call).cancel();

        decorated.enqueue(null);
        Mockito.verify(call).enqueue(any());

        decorated.isExecuted();
        Mockito.verify(call).isExecuted();

        decorated.isCanceled();
        Mockito.verify(call).isCanceled();

        decorated.request();
        Mockito.verify(call).request();

        decorated.execute();
        Mockito.verify(call).execute();
    }

    private interface StringCall extends Call<String> {
    }
}
