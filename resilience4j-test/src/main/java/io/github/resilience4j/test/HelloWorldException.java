package io.github.resilience4j.test;

public class HelloWorldException extends RuntimeException {

    public HelloWorldException() {
        super("BAM!");
    }

    public HelloWorldException(String message) {
        super(message);
    }
}
