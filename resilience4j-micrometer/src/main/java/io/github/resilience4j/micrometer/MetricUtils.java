package io.github.resilience4j.micrometer;

public class MetricUtils {
    public static String getName(String prefix, String name, String type) {
        return prefix + '.' + name + '.' + type;
    }
}
