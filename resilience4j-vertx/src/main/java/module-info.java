module resilience4j.vertx {
    requires vertx.core;
    requires resilience4j.circuitbreaker;
    requires resilience4j.core;
    exports io.github.resilience4j.vertx;
}