package io.github.resilience4j.customizer;

import java.util.List;

/**
 * Convenience class that encapsulate {@link Customizer} instances and use Delegate design pattern
 * to call them in sequence.
 *
 * @param <T> supported types that could be customized
 */
public class CompositeRegistryCustomizer<T> implements Customizer<T> {

    final private List<Customizer<T>> delegates;

    public CompositeRegistryCustomizer(List<Customizer<T>> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void customize(T registry) {
        delegates.stream().forEach(delegate -> delegate.customize(registry));
    }
}
