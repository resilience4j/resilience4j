package io.github.resilience4j.bulkhead.adaptive;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;

/**
 * @author romeh
 */
public class LimitResultTest {

	@Test
	public void testLimitResultDTO() {

		LimitResult limitResult = new LimitResult(2, 100l);
		LimitResult limitResult2 = new LimitResult(2, 100l);
		assertThat(limitResult.getLimit()).isEqualTo(limitResult2.getLimit());
		assertThat(limitResult.waitTime()).isEqualTo(limitResult2.waitTime());
		assertThat(limitResult.hashCode()).isEqualTo(limitResult2.hashCode());
		assertThat(limitResult.equals(limitResult2)).isTrue();
	}
}
