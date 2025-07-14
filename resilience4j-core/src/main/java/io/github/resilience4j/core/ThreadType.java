package io.github.resilience4j.core;

/**
 * Enumeration for thread types supported by Resilience4j.
 * 
 * <p>This enum represents the different types of threads that can be used
 * by Resilience4j's internal schedulers and executors.</p>
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
public enum ThreadType {
    
    /**
     * Virtual threads (Project Loom, JDK 21+).
     * These are lightweight threads that are managed by the JVM and can
     * provide better scalability for I/O-bound operations.
     */
    VIRTUAL("virtual"),
    
    /**
     * Traditional platform threads.
     * These are the standard OS threads that have been used historically.
     * This is the default thread type.
     */
    PLATFORM("platform");
    
    private final String value;
    
    ThreadType(String value) {
        this.value = value;
    }
    
    /**
     * Returns the string representation of this thread type.
     * 
     * @return the string value
     */
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Returns the default thread type.
     * 
     * @return {@link #PLATFORM} as the default
     */
    public static ThreadType getDefault() {
        return PLATFORM;
    }
    
    /**
     * Parses a thread type from a string value.
     * 
     * <p>The parsing is case-insensitive. If the input is null, empty, 
     * or contains only whitespace, the default type is returned.</p>
     * 
     * @param value the string value to parse
     * @return the corresponding ThreadType
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static ThreadType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getDefault();
        }
        
        String normalized = value.trim().toLowerCase();
        
        for (ThreadType type : values()) {
            if (type.value.equals(normalized)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("Unknown thread type: " + value + 
            ". Supported values are: " + VIRTUAL.value + ", " + PLATFORM.value);
    }
    
    /**
     * Safely parses a thread type from a string value, returning a default
     * value if parsing fails.
     * 
     * <p>This method never throws an exception. If the input cannot be parsed,
     * the provided default value is returned.</p>
     * 
     * @param value the string value to parse
     * @param defaultValue the default value to return if parsing fails
     * @return the parsed ThreadType or the default value
     */
    public static ThreadType fromStringOrDefault(String value, ThreadType defaultValue) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return fromString(value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}