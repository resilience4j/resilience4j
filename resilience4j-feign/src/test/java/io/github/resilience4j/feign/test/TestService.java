package io.github.resilience4j.feign.test;

import feign.RequestLine;


public interface TestService {

    @RequestLine("GET /greeting")
    String greeting();


}
