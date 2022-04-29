package io.github.resilience4j.cache;

import io.github.resilience4j.core.RegistryStore;

import javax.cache.Cache;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class CacheRegistryStore<E> implements RegistryStore<E> {

    private final Cache<String, E> cacheStore;

    public CacheRegistryStore(Cache<String, E> cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Override
    public E computeIfAbsent(String key, Function<? super String, ? extends E> mappingFunction) {
        try {
            return cacheStore.invoke(key, new AtomicComputeProcessor<>(), mappingFunction);
        } catch (EntryProcessorException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public E putIfAbsent(String key, E value) {
        return computeIfAbsent(key, k -> value);
    }

    @Override
    public Optional<E> find(String key) {
        return Optional.ofNullable(cacheStore.get(key));
    }

    @Override
    public Optional<E> remove(String name) {
        return Optional.ofNullable(cacheStore.getAndRemove(name));
    }

    @Override
    public Optional<E> replace(String name, E newEntry) {
        return Optional.ofNullable(cacheStore.getAndReplace(name, newEntry));
    }

    @Override
    public Collection<E> values() {
        Collection<E> values = new ArrayList<>();
        cacheStore.iterator().forEachRemaining(iter -> values.add(iter.getValue()));

        return values;
    }

    static class AtomicComputeProcessor<E> implements EntryProcessor<String, E, E> {

        @Override
        public E process(MutableEntry<String, E> entry, Object... arguments) throws EntryProcessorException {
            @SuppressWarnings("unchecked")
            Function<? super String, ? extends E> mappingFunction = (Function<? super String, ? extends E>) arguments[0];
            E oldValue = entry.getValue();
            if (oldValue != null) {
                return oldValue;
            }

            E newValue = mappingFunction.apply(entry.getKey());
            if (newValue != null) {
                entry.setValue(newValue);
                return newValue;
            } else {
                return oldValue;
            }
        }
    }
}
