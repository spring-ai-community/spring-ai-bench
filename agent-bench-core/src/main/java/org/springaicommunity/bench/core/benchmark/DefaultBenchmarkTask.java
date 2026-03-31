package org.springaicommunity.bench.core.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of {@link BenchmarkTask}.
 */
public record DefaultBenchmarkTask(String id, String instruction, Path workspaceTemplate, Path referenceDir,
		Map<String, Object> metadata, String difficulty, Duration timeout, List<String> setup,
		List<String> post) implements BenchmarkTask {

	public DefaultBenchmarkTask {
		metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
		setup = setup != null ? List.copyOf(setup) : List.of();
		post = post != null ? List.copyOf(post) : List.of();
	}

}
