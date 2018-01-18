module resilience4j.retrofit {
    requires resilience4j.ratelimiter;
    requires retrofit;
    requires vavr;
    requires okhttp;
    requires resilience4j.circuitbreaker;
    requires resilience4j.core;
    exports io.github.resilience4j.retrofit;
}