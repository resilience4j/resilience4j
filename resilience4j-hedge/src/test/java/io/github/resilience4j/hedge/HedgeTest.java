/*
 *
 *  Copyright 2026: Matthew Sandoz
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.internal.HedgeImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class HedgeTest {

    private static final String TEST_HEDGE = "TEST_HEDGE";

    @Test
    void shouldInitializeOfDefaults() {
        Hedge hedge = Hedge.ofDefaults();

        then(hedge.getHedgeConfig().toString()).isEqualTo(HedgeConfig.ofDefaults().toString());
        then(hedge.getName()).isEqualTo(HedgeImpl.DEFAULT_NAME);
    }

    @Test
    void shouldInitializeOfName() {
        Hedge hedge = Hedge.ofDefaults(TEST_HEDGE);

        then(hedge.getHedgeConfig().toString()).isEqualTo(HedgeConfig.ofDefaults().toString());
        then(hedge.getName()).isEqualTo(TEST_HEDGE);
    }

    @Test
    void shouldInitializeOfNameAndDefaults() {
        Hedge hedge = Hedge.of(TEST_HEDGE, HedgeConfig.ofDefaults());

        then(hedge.getName()).isEqualTo(TEST_HEDGE);
    }

    @Test
    void shouldInitializeConfig() {
        HedgeConfig config = HedgeConfig.ofDefaults();
        Hedge hedge = Hedge.of(config);

        then(hedge.getHedgeConfig()).isEqualTo(config);
    }

}
