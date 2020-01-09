package io.github.resilience4j.utils;

import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

public class ValueResolver {

    public static String resolve(StringValueResolver valueResolver, String value) {
        if (StringUtils.hasText(value)) {
            return valueResolver.resolveStringValue(value);
        }
        return value;
    }

}
