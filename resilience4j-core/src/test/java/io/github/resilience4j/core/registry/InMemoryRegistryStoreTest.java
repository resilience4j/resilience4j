/*
 * Copyright 2020 KrnSaurabh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.core.registry;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class InMemoryRegistryStoreTest {

    private static final String DEFAULT_CONFIG_VALUE = "defaultConfig";
    private static final String DEFAULT_CONFIG = "default";
    private static final String NEW_CONFIG = "newConfig";
    private static final String CUSTOM_CONFIG = "custom";
    private InMemoryRegistryStore<String> inMemoryRegistryStore;

    @Before
    public void initialiseInMemoryRegistryStore() {
        inMemoryRegistryStore = new InMemoryRegistryStore<>();
    }

    @Test
    public void shouldComputeValueWhenKeyNotPresentInRegistryStore() {
        assertEquals("Wrong Value",
            DEFAULT_CONFIG_VALUE, inMemoryRegistryStore.computeIfAbsent(DEFAULT_CONFIG, k -> DEFAULT_CONFIG_VALUE));
        assertThat(inMemoryRegistryStore.values()).hasSize(1);
        assertEquals("Wrong Value", DEFAULT_CONFIG_VALUE, inMemoryRegistryStore.computeIfAbsent(DEFAULT_CONFIG, k -> NEW_CONFIG));
    }

    @Test
    public void shouldPutKeyIntoRegistryStoreAndReturnOldValue() {
        assertNull(inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE));
        assertThat(inMemoryRegistryStore.values()).hasSize(1);
        assertEquals("Wrong Value", DEFAULT_CONFIG_VALUE, inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, NEW_CONFIG));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNPEWhenValueIsNull() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, null);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNPEWhenKeyIsNull() {
        inMemoryRegistryStore.putIfAbsent(null, DEFAULT_CONFIG_VALUE);
    }

    @Test
    public void shouldFindConfigFromRegistryStore() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.find(DEFAULT_CONFIG)).isNotEmpty();
        assertThat(inMemoryRegistryStore.find(DEFAULT_CONFIG)).hasValue(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.find(NEW_CONFIG)).isEmpty();
    }

    @Test
    public void shouldRemoveConfigFromRegistryStore() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        inMemoryRegistryStore.putIfAbsent(CUSTOM_CONFIG, NEW_CONFIG);
        assertThat(inMemoryRegistryStore.remove(DEFAULT_CONFIG)).hasValue(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.values()).hasSize(1);
    }

    @Test
    public void shouldReplaceKeyWithNewConfigValueWhenKeyPresent() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.replace(DEFAULT_CONFIG, NEW_CONFIG)).hasValue(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.find(DEFAULT_CONFIG)).hasValue(NEW_CONFIG);
    }

    @Test
    public void shouldNotReplaceKeyWithNewConfigValueWhenKeyAbsent() {
        assertThat(inMemoryRegistryStore.replace(NEW_CONFIG, NEW_CONFIG)).isEmpty();
        assertThat(inMemoryRegistryStore.values()).isEmpty();
    }

    @Test
    public void shouldReturnCollectionOfConfigs() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        inMemoryRegistryStore.putIfAbsent(CUSTOM_CONFIG, NEW_CONFIG);
        assertThat(inMemoryRegistryStore.values()).containsExactlyInAnyOrder(NEW_CONFIG, DEFAULT_CONFIG_VALUE);
    }

    // --- Concurrency tests for Issue #1 / #2 contract fixes ---

    @Test
    public void putIfAbsentShouldWaitForComputeIfAbsentAndReturnComputedValue() throws Exception {
        CountDownLatch computeStarted = new CountDownLatch(1);
        CountDownLatch computeCanFinish = new CountDownLatch(1);

        // Thread 1: computeIfAbsent with a slow mapping function
        CompletableFuture<String> computeFuture = CompletableFuture.supplyAsync(() ->
            inMemoryRegistryStore.computeIfAbsent("key1", k -> {
                computeStarted.countDown();
                try {
                    if (!computeCanFinish.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting for computeCanFinish latch");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "computed-value";
            })
        );

        // Wait until computeIfAbsent has started
        assertThat(computeStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // Thread 2: putIfAbsent while computeIfAbsent is still running
        AtomicReference<String> putResult = new AtomicReference<>();
        CompletableFuture<Void> putFuture = CompletableFuture.runAsync(() ->
            putResult.set(inMemoryRegistryStore.putIfAbsent("key1", "put-value"))
        );

        // Let computeIfAbsent finish
        computeCanFinish.countDown();

        computeFuture.get(5, TimeUnit.SECONDS);
        putFuture.get(5, TimeUnit.SECONDS);

        // putIfAbsent should have waited and returned the computed value
        assertThat(putResult.get()).isEqualTo("computed-value");
        // The stored value should be the computed one
        assertThat(inMemoryRegistryStore.find("key1")).hasValue("computed-value");
    }

    @Test
    public void removeShouldWaitForComputeIfAbsentAndReturnComputedValue() throws Exception {
        CountDownLatch computeStarted = new CountDownLatch(1);
        CountDownLatch computeCanFinish = new CountDownLatch(1);

        CompletableFuture<String> computeFuture = CompletableFuture.supplyAsync(() ->
            inMemoryRegistryStore.computeIfAbsent("key1", k -> {
                computeStarted.countDown();
                try {
                    if (!computeCanFinish.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting for computeCanFinish latch");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "computed-value";
            })
        );

        assertThat(computeStarted.await(5, TimeUnit.SECONDS)).isTrue();

        AtomicReference<java.util.Optional<String>> removeResult = new AtomicReference<>();
        CompletableFuture<Void> removeFuture = CompletableFuture.runAsync(() ->
            removeResult.set(inMemoryRegistryStore.remove("key1"))
        );

        computeCanFinish.countDown();

        computeFuture.get(5, TimeUnit.SECONDS);
        removeFuture.get(5, TimeUnit.SECONDS);

        // remove should have waited and returned the computed value
        assertThat(removeResult.get()).hasValue("computed-value");
        // Key should be removed
        assertThat(inMemoryRegistryStore.find("key1")).isEmpty();
    }

    @Test
    public void putIfAbsentShouldReturnNullWhenConcurrentComputeIfAbsentFails() throws Exception {
        CountDownLatch computeStarted = new CountDownLatch(1);
        CountDownLatch computeCanFinish = new CountDownLatch(1);

        // Thread 1: computeIfAbsent with a mapping function that will fail
        CompletableFuture<Void> computeFuture = CompletableFuture.runAsync(() -> {
            try {
                inMemoryRegistryStore.computeIfAbsent("key1", k -> {
                    computeStarted.countDown();
                    try {
                        if (!computeCanFinish.await(5, TimeUnit.SECONDS)) {
                            throw new AssertionError("Timed out waiting for computeCanFinish latch");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("Simulated failure in computeIfAbsent");
                });
            } catch (RuntimeException expected) {
                // Expected - computeIfAbsent propagates the exception to the winner thread
            }
        });

        // Wait until computeIfAbsent has started
        assertThat(computeStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // Thread 2: putIfAbsent while computeIfAbsent is still running (and will fail)
        AtomicReference<String> putResult = new AtomicReference<>("sentinel");
        CompletableFuture<Void> putFuture = CompletableFuture.runAsync(() ->
            putResult.set(inMemoryRegistryStore.putIfAbsent("key1", "put-value"))
        );

        // Let computeIfAbsent finish (and fail)
        computeCanFinish.countDown();

        computeFuture.get(5, TimeUnit.SECONDS);
        putFuture.get(5, TimeUnit.SECONDS);

        // putIfAbsent should return null (not throw CompletionException)
        // because the concurrent computeIfAbsent failed
        assertThat(putResult.get()).isNull();
    }

    @Test
    public void replaceShouldWaitForComputeIfAbsentAndReturnComputedValue() throws Exception {
        CountDownLatch computeStarted = new CountDownLatch(1);
        CountDownLatch computeCanFinish = new CountDownLatch(1);

        CompletableFuture<String> computeFuture = CompletableFuture.supplyAsync(() ->
            inMemoryRegistryStore.computeIfAbsent("key1", k -> {
                computeStarted.countDown();
                try {
                    if (!computeCanFinish.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting for computeCanFinish latch");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "computed-value";
            })
        );

        assertThat(computeStarted.await(5, TimeUnit.SECONDS)).isTrue();

        AtomicReference<java.util.Optional<String>> replaceResult = new AtomicReference<>();
        CompletableFuture<Void> replaceFuture = CompletableFuture.runAsync(() ->
            replaceResult.set(inMemoryRegistryStore.replace("key1", "replaced-value"))
        );

        computeCanFinish.countDown();

        computeFuture.get(5, TimeUnit.SECONDS);
        replaceFuture.get(5, TimeUnit.SECONDS);

        // replace should have waited and returned the old computed value
        assertThat(replaceResult.get()).hasValue("computed-value");
        // The stored value should now be the replacement
        assertThat(inMemoryRegistryStore.find("key1")).hasValue("replaced-value");
    }
}
