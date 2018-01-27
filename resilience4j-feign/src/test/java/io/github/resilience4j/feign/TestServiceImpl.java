package io.github.resilience4j.feign;

public class TestServiceImpl implements TestService {

    @Override
    public String greeting() {
        return "testGreeting";
    }

}
