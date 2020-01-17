package io.github.resilience4j.service.test;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

@Component
public class TimeLimiterDummyServiceImpl implements TimeLimiterDummyService {

    @TimeLimiter(name = TimeLimiterDummyService.BACKEND)
    @Override
    public void doSomething(boolean timeout) throws Exception {
        if (timeout) {
            Thread.sleep(2000);
        }
    }
}
