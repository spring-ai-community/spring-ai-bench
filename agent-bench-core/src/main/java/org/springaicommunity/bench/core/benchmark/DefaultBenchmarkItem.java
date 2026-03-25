package org.springaicommunity.bench.core.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Immutable implementation of {@link BenchmarkItem}.
 */
public record DefaultBenchmarkItem(String id, String instruction, Path workspaceTemplate, Path referenceDir,
		Map<String, Object> metadata, Duration timeout, List<String> setup, List<String> post)
		implements BenchmarkItem {

	public DefaultBenchmarkItem {
		metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
		setup = setup != null ? List.copyOf(setup) : List.of();
		post = post != null ? List.copyOf(post) : List.of();
	}

	/** Backward-compatible constructor without setup/post. */
	public DefaultBenchmarkItem(String id, String instruction, Path workspaceTemplate, Path referenceDir,
			Map<String, Object> metadata, Duration timeout) {
		this(id, instruction, workspaceTemplate, referenceDir, metadata, timeout, List.of(), List.of());
	}

}
