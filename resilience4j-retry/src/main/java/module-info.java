module resilience4j.retry {
    requires transitive vavr;
    requires resilience4j.core;
    exports io.github.resilience4j.retry;
    exports io.github.resilience4j.retry.event;
}