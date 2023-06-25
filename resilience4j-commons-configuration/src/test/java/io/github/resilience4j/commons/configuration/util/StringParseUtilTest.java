package io.github.resilience4j.commons.configuration.util;

import junit.framework.TestCase;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class StringParseUtilTest {
    @Test
    public void testExtractUniquePrefixes() {
        Iterator<String> iterator = List.of("backendA.circuitBreaker.test1", "backendA.circuitBreaker.test2", "backendB.circuitBreaker.test3").iterator();

        Set<String> prefixes = StringParseUtil.extractUniquePrefixes(iterator, ".");

        Assertions.assertThat(prefixes).containsExactlyInAnyOrder("backendA", "backendB");
    }
}