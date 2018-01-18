module resilience4j.spring.boot {
    requires spring.core;
    requires resilience4j.ratelimiter;
    requires aspectjweaver;
    requires slf4j.api;
    requires resilience4j.circuitbreaker;
    requires resilience4j.consumer;
    requires spring.context;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.beans;
}