package org.springaicommunity.bench.core.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Immutable implementation of {@link BenchmarkItem}.
 */
public record DefaultBenchmarkItem(String id, String instruction, Path workspaceTemplate, Path referenceDir,
		Map<String, Object> metadata, Duration timeout) implements BenchmarkItem {

	public DefaultBenchmarkItem {
		metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
	}

}
