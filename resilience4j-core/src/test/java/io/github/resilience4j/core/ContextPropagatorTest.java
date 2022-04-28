/*
 *
 *  Copyright 2020 krnsaurabh
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
package io.github.resilience4j.core;

import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.Test;

import io.github.resilience4j.core.TestContextPropagators.TestThreadLocalContextPropagator;

public class ContextPropagatorTest {

    @Test
    public void contextPropagationFailureSingleTest() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        Supplier<String> supplier = threadLocal::get;
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue(null)));
    }

    @Test
    public void contextPropagationEmptyListShouldNotFail() {
        Supplier<String> supplier = () -> "Hello World";

        //Thread boundary
        Supplier<String> decoratedSupplier = ContextPropagator.decorateSupplier(Collections.emptyList(), supplier);
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("Hello World")));
    }

    @Test
    public void contextPropagationEmptyListShouldNotFailWithCallable() {
        //Thread boundary
        Callable<String> decorateCallable = ContextPropagator.decorateCallable(Collections.emptyList(), () -> "Hello World");

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(decorateCallable.call()).isEqualTo("Hello World")));
    }

    @Test
    public void contextPropagationFailureMultipleTest() {
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShould_NOT_CrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShould_NOT_CrossThreadBoundary");

        Supplier<List<String>> supplier = () -> Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        );
        //Thread boundary
        final CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future.get()).containsExactlyInAnyOrder(null, null)));
    }

    @Test
    public void contextPropagationSupplierMultipleTest() {
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        Supplier<List<String>> supplier = ContextPropagator.decorateSupplier(
            Arrays.asList(propagatorOne, propagatorTwo),
            () -> Arrays.asList(threadLocalOne.get(), threadLocalTwo.get()));
        //Thread boundary
        final CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
        ));
    }

    @Test
    public void contextPropagationSupplierMultipleTestWithCallable() {
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        Callable<List<String>> callable = ContextPropagator.decorateCallable(
            Arrays.asList(propagatorOne, propagatorTwo),
            () -> Arrays.asList(threadLocalOne.get(), threadLocalTwo.get()));
        //Thread boundary

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(callable.call()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
        ));
    }

    @Test
    public void contextPropagationSupplierSingleTest() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        Supplier<String> supplier = ContextPropagator.decorateSupplier(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("SingleValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationSupplierSingleTestWithCallable() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        Callable<String> callable = ContextPropagator.decorateCallable(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);

        waitAtMost(200, TimeUnit.MILLISECONDS).until(matches(() ->
            assertThat(callable.call()).isEqualTo("SingleValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableFailureSingleTest() {
        AtomicReference<String> reference = new AtomicReference<>();
        //Thread boundary
        Runnable runnable = ContextPropagator.decorateRunnable(
            Collections.emptyList(),
            () -> reference.set("Hello World"));

        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue("Hello World")));
    }

    @Test
    public void contextPropagationRunnableEmptyListShouldNotFail() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        AtomicReference<String> reference = new AtomicReference<>();
        Runnable runnable = () -> reference.set(threadLocal.get());
        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue(null)));
    }

    @Test
    public void contextPropagationRunnableSingleTest() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        AtomicReference<String> reference = new AtomicReference<>();
        Runnable runnable = ContextPropagator.decorateRunnable(
            new TestThreadLocalContextPropagator(threadLocal),
            () -> reference.set(threadLocal.get()));
        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue("SingleValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableMultipleTest() {
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        AtomicReference<List<String>> reference = new AtomicReference<>();
        Runnable runnable = ContextPropagator.decorateRunnable(
            Arrays.asList(propagatorOne, propagatorTwo),
            () -> reference.set(Arrays.asList(
                threadLocalOne.get(),
                threadLocalTwo.get()
            )));

        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableMultipleFailureTest() {
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCross_NOT_ThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShould_NOT_CrossThreadBoundary");

        AtomicReference<List<String>> reference = new AtomicReference<>();
        Runnable runnable = () -> reference.set(Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        ));

        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference.get()).containsExactlyInAnyOrder(null, null)));
    }
}
