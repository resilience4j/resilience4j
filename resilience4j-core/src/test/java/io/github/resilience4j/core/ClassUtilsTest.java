package io.github.resilience4j.core;

import org.junit.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClassUtilsTest {

    @Test
    public void shouldInstantiatePredicateClass() {
        assertThat(ClassUtils.instantiatePredicateClass(PublicPredicate.class)).isNotNull();
    }

    @Test
    public void shouldFailToInstantiatePredicateClass() {
        assertThatThrownBy(
            () -> ClassUtils.instantiatePredicateClass(NoDefaultConstructorPredicate.class))
            .isInstanceOf(InstantiationException.class)
            .hasCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    public void shouldInstantiateClassWithDefaultConstructor() {
        assertThat(ClassUtils.instantiateClassDefConstructor(DefaultConstructor.class)).isNotNull();
    }

    @Test
    public void shouldInstantiateClassWithDefaultConstructor2() {
        assertThat(ClassUtils.instantiateClassDefConstructor(DefaultConstructor2.class)).isNotNull();
    }

    @Test
    public void shouldFailToInstantiateNoDefaultConstructor() {
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

        public NoDefaultConstructorPredicate(String bla) {

            this.bla = bla;
        }

        @Override
        public boolean test(String o) {
            return o.equals(bla);
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
