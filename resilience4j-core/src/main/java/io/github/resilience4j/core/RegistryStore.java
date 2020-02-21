package io.github.resilience4j.core;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public interface RegistryStore<E> {

    E computeIfAbsent(String key,
        Function<? super String, ? extends E> mappingFunction);

    E putIfAbsent(String key, E value);

    Optional<E> find(String key);

    Optional<E> remove(String name);

    Optional<E> replace(String name, E newEntry);

    Collection<E> values();

}
