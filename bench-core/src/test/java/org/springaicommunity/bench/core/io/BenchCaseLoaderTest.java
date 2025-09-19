package org.springaicommunity.bench.core.io;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springaicommunity.bench.core.spec.BenchCase;

class BenchCaseLoaderTest {

	@Test
	void shouldLoadSampleYaml() throws Exception {
		Path yaml = Path.of("src/test/resources/samples/calculator-sqrt-bug.yaml");
		BenchCase bc = BenchCaseLoader.load(yaml);

		assertEquals("calculator-sqrt-bug", bc.id());
		assertEquals("coding", bc.category());
		assertEquals("rd-1-2022", bc.repo().owner());
		assertTrue(bc.agent().prompt().contains("Fix the failing JUnit tests"));
		assertEquals("mvn test", bc.success().cmd());
	}

}
