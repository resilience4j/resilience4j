package io.github.resilience4j.rxjava3.micrometer.transformer;

/**
 * Wraps a value to prevent the same values be treated as equal when adding to Set.
 *
 * @param <T> value type
 */
class ValueWrapper<T> {

    private final T value;

    ValueWrapper(T value) {
        this.value = value;
    }

    T getValue() {
        return value;
    }
}
