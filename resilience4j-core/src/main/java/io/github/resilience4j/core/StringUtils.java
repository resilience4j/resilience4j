package io.github.resilience4j.core;

import io.github.resilience4j.core.lang.Nullable;

public class StringUtils {

    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

}
