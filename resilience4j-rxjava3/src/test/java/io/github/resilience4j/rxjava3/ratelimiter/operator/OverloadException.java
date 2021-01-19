package io.github.resilience4j.rxjava3.ratelimiter.operator;

class OverloadException extends RuntimeException {

    static class SpecificOverloadException extends OverloadException {
    }
}
