/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.github.resilience4j.test.HelloWorldService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

public class ThreadPoolBulkheadTest {

    private HelloWorldService helloWorldService;
    private ThreadPoolBulkheadConfig config;

    @Before
    public void setUp(){
        helloWorldService = Mockito.mock(HelloWorldService.class);
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(1)
            .queueCapacity(10)
            .build();
    }

    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess() throws ExecutionException, InterruptedException {

        // Given
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);

        BDDMockito.given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        // When
        Future<String> result = bulkhead.executeSupplier(helloWorldService::returnHelloWorld);

        // Then
        assertThat(result.get()).isEqualTo("Hello world");
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        BDDMockito.then(helloWorldService).should(times(1)).returnHelloWorld();
    }



}
