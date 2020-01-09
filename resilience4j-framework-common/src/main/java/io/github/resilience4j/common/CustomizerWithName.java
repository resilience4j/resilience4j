package io.github.resilience4j.common;

/**
 * common interface for different spring config customizers implementation
 */
public interface CustomizerWithName {

    /**
     * @return name of the resilience4j type instance to be customized
     */
    String name();
}
