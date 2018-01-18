module resilience4j.bulkhead {
    requires transitive vavr;
    requires resilience4j.core;
    exports io.github.resilience4j.bulkhead;
    exports io.github.resilience4j.bulkhead.event;
    exports io.github.resilience4j.bulkhead.utils;
}