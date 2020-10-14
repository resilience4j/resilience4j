package io.github.resilience4j.core.functions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Apply a computation only once.
 *
 * @param <T> the type of the input which is passed to the consumer and executed once.
 */
public final class OnceConsumer<T> {

    final T t;
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    private OnceConsumer(T t) {
        this.t = t;
    }

    /**
     * Create a do once consumer.
     *
     * @param t   input which is passed to operation
     * @param <T> type of input on which operation is applied.
     * @return
     */
    public static <T> OnceConsumer<T> of(T t) {
        return new OnceConsumer<>(t);
    }

    /**
     * Apply a computation on subject only once.
     * <pre><code>
     * List&lt;String&gt; lst = new ArrayList&lt;&gt;();
     *
     * OnceConsumer&lt;List&lt;String&gt;&gt; once = OnceConsumer.of(lst);
     * once.applyOnce((l) -&gt; l.add("Hello World"));
     * once.applyOnce((l) -&gt; l.add("Hello World"));
     *
     * assertThat(lst).hasSize(1).contains("Hello World");
     *
     * </code></pre>
     *
     * @param consumer computation run once with input t
     */
    public void applyOnce(Consumer<T> consumer) {
        if (hasRun.compareAndSet(false, true)) {
            consumer.accept(t);
        }
    }
}