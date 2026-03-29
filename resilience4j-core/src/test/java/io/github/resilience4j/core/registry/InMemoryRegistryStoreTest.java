/*
 * Copyright 2026 KrnSaurabh
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InMemoryRegistryStoreTest {

    private static final String DEFAULT_CONFIG_VALUE = "defaultConfig";
    private static final String DEFAULT_CONFIG = "default";
    private static final String NEW_CONFIG = "newConfig";
    private static final String CUSTOM_CONFIG = "custom";
    private InMemoryRegistryStore<String> inMemoryRegistryStore;

    @BeforeEach
    void initialiseInMemoryRegistryStore() {
        inMemoryRegistryStore = new InMemoryRegistryStore<>();
    }

    @Test
    void shouldComputeValueWhenKeyNotPresentInRegistryStore() {
        assertThat(inMemoryRegistryStore.computeIfAbsent(DEFAULT_CONFIG, k -> DEFAULT_CONFIG_VALUE)).as("Wrong Value").isEqualTo(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.values()).hasSize(1);
        assertThat(inMemoryRegistryStore.computeIfAbsent(DEFAULT_CONFIG, k -> NEW_CONFIG)).as("Wrong Value").isEqualTo(DEFAULT_CONFIG_VALUE);
    }

    @Test
    void shouldPutKeyIntoRegistryStoreAndReturnOldValue() {
        assertThat(inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE)).isNull();
        assertThat(inMemoryRegistryStore.values()).hasSize(1);
        assertThat(inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, NEW_CONFIG)).as("Wrong Value").isEqualTo(DEFAULT_CONFIG_VALUE);
    }

    @Test
    void shouldThrowNPEWhenValueIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
            inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, null));
    }

    @Test
    void shouldThrowNPEWhenKeyIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() ->
            inMemoryRegistryStore.putIfAbsent(null, DEFAULT_CONFIG_VALUE));
    }

    @Test
    void shouldFindConfigFromRegistryStore() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.find(DEFAULT_CONFIG)).isPresent();
        assertThat(inMemoryRegistryStore.find(DEFAULT_CONFIG)).hasValue(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.find(NEW_CONFIG)).isEmpty();
    }

    @Test
    void shouldRemoveConfigFromRegistryStore() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        inMemoryRegistryStore.putIfAbsent(CUSTOM_CONFIG, NEW_CONFIG);
        assertThat(inMemoryRegistryStore.remove(DEFAULT_CONFIG)).hasValue(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.values()).hasSize(1);
    }

    @Test
    void shouldReplaceKeyWithNewConfigValueWhenKeyPresent() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.replace(DEFAULT_CONFIG, NEW_CONFIG)).hasValue(DEFAULT_CONFIG_VALUE);
        assertThat(inMemoryRegistryStore.find(DEFAULT_CONFIG)).hasValue(NEW_CONFIG);
    }

    @Test
    void shouldNotReplaceKeyWithNewConfigValueWhenKeyAbsent() {
        assertThat(inMemoryRegistryStore.replace(NEW_CONFIG, NEW_CONFIG)).isEmpty();
        assertThat(inMemoryRegistryStore.values()).isEmpty();
    }

    @Test
    void shouldReturnCollectionOfConfigs() {
        inMemoryRegistryStore.putIfAbsent(DEFAULT_CONFIG, DEFAULT_CONFIG_VALUE);
        inMemoryRegistryStore.putIfAbsent(CUSTOM_CONFIG, NEW_CONFIG);
        assertThat(inMemoryRegistryStore.values()).containsExactlyInAnyOrder(NEW_CONFIG, DEFAULT_CONFIG_VALUE);
    }
}
