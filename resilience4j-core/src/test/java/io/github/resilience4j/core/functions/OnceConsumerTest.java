package io.github.resilience4j.core.functions;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

/**
 * Class OnceConsumer test.
 */
public class OnceConsumerTest {

    @Test
    public void shouldApplyOnlyOnce() {
        final List<String> lst = new ArrayList<>();
        OnceConsumer<List<String>> once = OnceConsumer.of(lst);
        once.applyOnce((l) -> l.add("Hello World"));
        once.applyOnce((l) -> l.add("Hello World"));

        assertThat(lst).hasSize(1).contains("Hello World");
    }

    @Test
    public void shouldRunOnlyOnceWithException() {
        final List<String> lst = new ArrayList<>();
        OnceConsumer<List<String>> once = OnceConsumer.of(lst);
        Consumer<List<String>> blowUp = (l) -> {
            throw new RuntimeException("BAM!");
        };
        Throwable cause = catchThrowable(() -> once.applyOnce(blowUp));

        assertThat(cause).isInstanceOf(RuntimeException.class).hasMessage("BAM!");
        assertThatCode(() -> once.applyOnce(blowUp)).doesNotThrowAnyException();
    }
}