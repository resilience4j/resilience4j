module resilience4j.circuitbreaker {
    requires transitive vavr;
    requires resilience4j.core;
    exports io.github.resilience4j.circuitbreaker;
    exports io.github.resilience4j.circuitbreaker.event;
    exports io.github.resilience4j.circuitbreaker.utils;
}