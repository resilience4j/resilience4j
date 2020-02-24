package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.Optional;

public class ValueResolver {

    public static String resolve(@Nullable StringValueResolver valueResolver, String value) {
        if (StringUtils.hasText(value) && valueResolver != null) {
            return Optional.ofNullable(valueResolver.resolveStringValue(value)).orElse(value);
        }
        return value;
    }

}
