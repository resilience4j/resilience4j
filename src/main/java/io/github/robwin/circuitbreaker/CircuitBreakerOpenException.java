package io.github.robwin.circuitbreaker;
// Project:   Deutsche Telekom - SPICA
// Author:    Florin Pinte <Florin.Pinte@qaware.de>
// Copyright: QAware GmbH

/**
 * Basic trouble that indicates that the backend (email core, address book, calendar) failed to perform its operation.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    /**
     * The constructor with a message.
     *
     * @param message The message.
     */
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}


