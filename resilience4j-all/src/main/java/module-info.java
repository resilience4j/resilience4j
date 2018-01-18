module resilience4j.all{
    requires transitive vavr;
    requires resilience4j.circuitbreaker;
    requires resilience4j.retry;
    requires resilience4j.ratelimiter;
    requires resilience4j.cache;
    requires resilience4j.bulkhead;
}