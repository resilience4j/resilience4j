/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.bulkhead.configure;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vavr.CheckedFunction0;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * aspect unit test
 */
@RunWith(MockitoJUnitRunner.class)
public class RxJava2BulkheadAspectExtTest {

    @InjectMocks
    RxJava2BulkheadAspectExt rxJava2BulkheadAspectExt;

    @Test
    public void testSingleType() throws Throwable {
        Bulkhead bulkhead = Bulkhead.ofDefaults("test");
        CheckedFunction0<Object> decorated = rxJava2BulkheadAspectExt.decorate(bulkhead, () -> Single.just("Test"));
        assertThat(decorated.apply()).isInstanceOf(Single.class);
    }

    @Test
    public void testFlowableType() throws Throwable {
        Bulkhead bulkhead = Bulkhead.ofDefaults("test");
        CheckedFunction0<Object> decorated = rxJava2BulkheadAspectExt.decorate(bulkhead, () -> Flowable.just("Test"));
        assertThat(decorated.apply()).isInstanceOf(Flowable.class);
    }
}
