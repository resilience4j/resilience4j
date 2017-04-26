/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BulkheadOperatorTest {

    @Test
    public void shouldReturnOnCompleteUsingSingle() {

        // Given
        Bulkhead bulkhead  = Bulkhead.of("test", 1);

        Single.just(1)
              .lift(BulkheadOperator.of(bulkhead))
              .test()
              .assertValueCount(1)
              .assertValues(1)
              .assertComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingUsingSingle() {

        // Given
        Bulkhead bulkhead  = Bulkhead.of("test", 1);

        Single.fromCallable(() -> {throw new IOException("BAM!");})
              .lift(BulkheadOperator.of(bulkhead))
              .test()
              .assertError(IOException.class)
              .assertNotComplete()
              .assertSubscribed();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingObservable() {

        // Given
        Bulkhead bulkhead  = Bulkhead.of("test", 1);

        // When
        Observable.fromArray("Event 1", "Event 2")
                  .lift(BulkheadOperator.of(bulkhead))
                  .test()
                  .assertValueCount(2)
                  .assertValues("Event 1", "Event 2")
                  .assertComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnCompleteUsingFlowable() {

        // Given
        Bulkhead bulkhead  = Bulkhead.of("test", 1);

        // When
        Flowable.fromArray("Event 1", "Event 2")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertValueCount(2)
                .assertValues("Event 1", "Event 2")
                .assertComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingObservable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        // When
        Observable.fromCallable(() -> {throw new IOException("BAM!");})
                  .lift(BulkheadOperator.of(bulkhead))
                  .test()
                  .assertError(IOException.class)
                  .assertNotComplete()
                  .assertSubscribed();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorUsingFlowable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);

        // When
        Flowable.fromCallable(() -> {throw new IOException("BAM!");})
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(IOException.class)
                .assertNotComplete()
                .assertSubscribed();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithBulkheadFullExceptionUsingObservable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        bulkhead.isCallPermitted();

        // When
        Observable.fromArray("Event 1", "Event 2")
                  .lift(BulkheadOperator.of(bulkhead))
                  .test()
                  .assertError(BulkheadFullException.class)
                  .assertNotComplete()
                  .assertSubscribed();

        bulkhead.onComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithBulkheadFullExceptionUsingFlowable() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        bulkhead.isCallPermitted();

        // When
        Flowable.fromArray("Event 1", "Event 2")
                .lift(BulkheadOperator.of(bulkhead))
                .test()
                .assertError(BulkheadFullException.class)
                .assertNotComplete()
                .assertSubscribed();

        bulkhead.onComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

    @Test
    public void shouldReturnOnErrorWithBulkheadFullExceptionUsingSingle() {

        // Given
        Bulkhead bulkhead = Bulkhead.of("test", 1);
        bulkhead.isCallPermitted();

        // When
        Single.just("foobar")
              .lift(BulkheadOperator.of(bulkhead))
              .test()
              .assertError(BulkheadFullException.class);

        bulkhead.onComplete();

        // Then
        assertThat(bulkhead.getRemainingDepth()).isEqualTo(1);
    }

}
