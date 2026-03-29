package io.github.resilience4j.core;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassUtilsTest {

    @Test
    void shouldInstantiatePredicateClass() {
        assertThat(ClassUtils.instantiatePredicateClass(PublicPredicate.class)).isNotNull();
    }

    @Test
    void shouldFailToInstantiatePredicateClass() {
        assertThatThrownBy(
            () -> ClassUtils.instantiatePredicateClass(NoDefaultConstructorPredicate.class))
            .isInstanceOf(InstantiationException.class)
            .hasCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void shouldInstantiateBiConsumerClass(){
        assertThat(ClassUtils.instantiateBiConsumer(PublicBiConsumer.class)).isNotNull();
    }

    @Test
    void shouldFailToInstantiateBiConsumerClassWithoutDefaultConstructor(){
        assertThatThrownBy(
            () -> ClassUtils.instantiateBiConsumer(NoDefaultConstructorBiConsumer.class))
            .isInstanceOf(InstantiationException.class)
            .hasCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void shouldInstantiateClassWithDefaultConstructor() {
        assertThat(ClassUtils.instantiateClassDefConstructor(DefaultConstructor.class)).isNotNull();
    }

    @Test
    void shouldInstantiateClassWithDefaultConstructor2() {
        assertThat(ClassUtils.instantiateClassDefConstructor(DefaultConstructor2.class)).isNotNull();
    }

    @Test
    void shouldFailToInstantiateNoDefaultConstructor() {
        assertThatThrownBy(
            () -> ClassUtils.instantiateClassDefConstructor(NoDefaultConstructor.class))
            .isInstanceOf(InstantiationException.class);
    }

    public static class PublicPredicate implements Predicate<String> {

        @Override
        public boolean test(String o) {
            return false;
        }
    }

    private static class NoDefaultConstructorPredicate implements Predicate<String> {

        private String bla;

        @Override
        public boolean test(String o) {
            return o.equals(bla);
        }
    }

    public static class PublicBiConsumer implements BiConsumer<Integer, String>{

        @Override
        public void accept(Integer integer, String s) {

        }
    }

    public static class NoDefaultConstructorBiConsumer extends PublicBiConsumer {

        public NoDefaultConstructorBiConsumer(String foo) {
        }
    }

    public static class NoDefaultConstructor  {
        public NoDefaultConstructor(String a){}
    }

    public static class DefaultConstructor  {}

    public static class DefaultConstructor2  {
        public DefaultConstructor2(String a){}
        public DefaultConstructor2(){}
    }
}
