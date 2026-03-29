package org.springaicommunity.bench.core.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * A single item within a benchmark that provides an instruction and workspace for an
 * agent to work on.
 */
public interface BenchmarkItem {

	String id();

	String instruction();

	Path workspaceTemplate();

	Path referenceDir();

	Map<String, Object> metadata();

	Duration timeout();

}
