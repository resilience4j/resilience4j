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
}
