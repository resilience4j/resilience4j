package io.github.resilience4j.bulkhead.adaptive.internal;

interface StateMachine {

    void transitionToCongestionAvoidance();

    void transitionToSlowStart();

}
