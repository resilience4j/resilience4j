/*
 * Copyright 2019 Mahmoud Romeh
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

import io.micrometer.core.lang.Nullable;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/**
 * Integer to duration spring converter , will convert to milliseconds
 */
@Component
@ConfigurationPropertiesBinding
@Order(0)
public class IntegerToDurationConverter implements Converter<Integer, Duration> {

    @Override
    public Duration convert(@Nullable Integer source) {
        if (source != null) {
            try {
                return Duration.ofMillis(source);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Cannot convert '" + source + "' to Duration",
                    e);
            }
        } else {
            return null;
        }

    }
}
