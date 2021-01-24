package io.github.resilience4j.reactor.ratelimiter.operator;

class ResponseWithPotentialOverload {

    private final boolean overload;

    ResponseWithPotentialOverload(boolean overload) {
        this.overload = overload;
    }

    boolean isOverload() {
        return overload;
    }

    static class SpecificResponseWithPotentialOverload extends ResponseWithPotentialOverload {

        SpecificResponseWithPotentialOverload(boolean overload) {
            super(overload);
        }
    }
}
