package io.github.resilience4j.reactor;

/**
 * Represents the possible states of a permit.
 */
public enum Permit {
    PENDING, ACQUIRED, REJECTED
}
