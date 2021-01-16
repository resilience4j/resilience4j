package io.github.resilience4j.reactor.ratelimiter.operator;

class OverloadException extends RuntimeException {

    static class SpecificOverloadException extends OverloadException {
    }
}
