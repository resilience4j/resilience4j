module resilience4j.ratelimiter {
    requires transitive vavr;
    requires resilience4j.core;
    exports io.github.resilience4j.ratelimiter;
    exports io.github.resilience4j.ratelimiter.event;
}