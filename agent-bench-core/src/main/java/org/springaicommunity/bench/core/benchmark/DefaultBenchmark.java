package org.springaicommunity.bench.core.benchmark;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of {@link Benchmark}.
 */
public record DefaultBenchmark(String name, String version, Map<String, Object> metadata, List<BenchmarkTask> tasks,
		Map<String, Object> juryConfig, Duration defaultTimeout) implements Benchmark {

	public DefaultBenchmark {
		metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
		tasks = List.copyOf(tasks);
		juryConfig = juryConfig != null ? Map.copyOf(juryConfig) : Map.of();
		if (defaultTimeout == null) {
			defaultTimeout = Duration.ofMinutes(5);
		}
	}

}
