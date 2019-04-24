package io.github.resilience4j.utils;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

/**
 * unit tes for the util class
 */
public class CommonUtilsTest {


	@Test
	public void testMergeProperties() {
		Pojo pojo1 = new Pojo("Test1", "Test2");
		Pojo pojo2 = new Pojo("Test3", "Test4");
		final Pojo pojo = CommonUtils.mergeProperties(pojo1, pojo2);
		assertThat(pojo.property1).isEqualTo("Test1");
		assertThat(pojo.property2).isEqualTo("Test2");
	}

	@Test
	public void testGetNullProperties() {
		Pojo pojo1 = new Pojo("Test1", null);
		Pojo pojo2 = new Pojo("Test3", "Test4");
		final Pojo pojo = CommonUtils.mergeProperties(pojo1, pojo2);
		assertThat(pojo.property1).isEqualTo("Test1");
		assertThat(pojo.property2).isEqualTo("Test4");
	}


	class Pojo {

		private String property1;
		private String property2;

		Pojo(String property1, String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}

		public void setProperty1(String property1) {
			this.property1 = property1;
		}

		public void setProperty2(String property2) {
			this.property2 = property2;
		}

		public String getProperty1() {
			return property1;
		}

		public String getProperty2() {
			return property2;
		}

	}

}