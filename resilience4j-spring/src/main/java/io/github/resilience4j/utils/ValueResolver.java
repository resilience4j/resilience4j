package io.github.resilience4j.utils;

import io.github.resilience4j.core.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

public class ValueResolver {

    @Nullable
    public static String resolve(@Nullable StringValueResolver valueResolver, String value) {
        if (StringUtils.hasText(value)) {
            if(valueResolver != null){
                return valueResolver.resolveStringValue(value);
            }
        }
        return value;
    }

}
