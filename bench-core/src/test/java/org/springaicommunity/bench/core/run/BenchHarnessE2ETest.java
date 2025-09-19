package org.springaicommunity.bench.core.run;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.springaicommunity.bench.core.io.BenchCaseLoader;
import org.springaicommunity.bench.core.spec.BenchCase;

class BenchHarnessE2ETest {

	@Test
	void calculatorCaseGoesGreen() throws Exception {
		Path yaml = Path.of("src/test/resources/samples/calculator-sqrt-bug.yaml");
		BenchCase bc = BenchCaseLoader.load(yaml);
		BenchHarness harness = new BenchHarness(GitHub.connectAnonymously());

		BenchResult res = harness.run(bc);
		assertTrue(res.passed(), "Agent should fix tests");
	}

}
