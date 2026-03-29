package org.springaicommunity.bench.core.benchmark;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springaicommunity.judge.Judge;

/**
 * Smoke test for the code-coverage benchmark definition. Validates that the benchmark
 * YAML is discoverable, parseable, and the jury config wires correctly through
 * JudgeFactory.
 */
class CodeCoverageBenchmarkTest {

	@Test
	void benchmarkDiscoverable() throws IOException {
		BenchmarkCatalog catalog = new BenchmarkCatalog(Path.of("../benchmarks"));
		List<Benchmark> benchmarks = catalog.discover();

		assertThat(benchmarks).extracting(Benchmark::name).contains("code-coverage");
	}

	@Test
	void benchmarkHasSpringPetclinicItem() throws IOException {
		BenchmarkCatalog catalog = new BenchmarkCatalog(Path.of("../benchmarks"));
		Benchmark benchmark = catalog.discover()
			.stream()
			.filter(b -> b.name().equals("code-coverage"))
			.findFirst()
			.orElseThrow();

		assertThat(benchmark.tasks()).hasSize(1);
		BenchmarkTask task = benchmark.tasks().get(0);
		assertThat(task.id()).isEqualTo("spring-petclinic");
		assertThat(task.instruction()).contains("JUnit tests");
		assertThat(task.metadata()).containsKey("baselineCoverage");
	}

	@Test
	void juryConfigWiresThroughJudgeFactory() throws IOException {
		BenchmarkCatalog catalog = new BenchmarkCatalog(Path.of("../benchmarks"));
		Benchmark benchmark = catalog.discover()
			.stream()
			.filter(b -> b.name().equals("code-coverage"))
			.findFirst()
			.orElseThrow();

		Map<String, Object> juryConfig = benchmark.juryConfig();
		assertThat(juryConfig).containsKey("tiers");

		// The factory should create a judge from the config — but test-quality-llm
		// is not registered in core. It will fail on that tier. So we test only
		// that the first 3 tiers (built-in types) parse without error.
		JudgeFactory factory = new JudgeFactory();
		// Register a stub for test-quality-llm so the full cascade can be created
		factory.register("test-quality-llm", config -> (ctx) -> null);

		Judge judge = factory.createFromConfig(juryConfig);
		assertThat(judge).isNotNull();
	}

}
