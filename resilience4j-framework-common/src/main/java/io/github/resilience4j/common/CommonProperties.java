/*
 * Copyright Mahmoud Romeh
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
package io.github.resilience4j.common;

import java.util.HashMap;
import java.util.Map;

/**
 * common properties between different spring resilience4j supported types
 */
public class CommonProperties {

    /**
     * The Optional configured global registry tags if any that can be used with the exported
     * metrics
     */
    Map<String, String> tags = new HashMap<>();

    /**
     * @return the Optional configured registry global tags if any that can be used with the
     * exported metrics
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * @param tags the optional configured tags values into registry
     */
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
}
