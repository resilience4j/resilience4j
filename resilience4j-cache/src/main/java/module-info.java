module resilience4j.cache {
    requires slf4j.api;
    requires transitive cache.api;
    requires transitive vavr;
    requires resilience4j.core;
    exports io.github.resilience4j.cache;
    exports io.github.resilience4j.cache.event;
}