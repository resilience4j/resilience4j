module resilience4j.metrics{
    requires metrics.core;
    requires resilience4j.circuitbreaker;
    requires transitive vavr;
    requires resilience4j.ratelimiter;
    requires resilience4j.bulkhead;
}