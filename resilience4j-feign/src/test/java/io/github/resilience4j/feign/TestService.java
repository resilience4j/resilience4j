package io.github.resilience4j.feign;

import feign.RequestLine;


public interface TestService {

    @RequestLine("GET /greeting")
    String greeting();


}
