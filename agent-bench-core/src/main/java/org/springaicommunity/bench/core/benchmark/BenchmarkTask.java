package org.springaicommunity.bench.core.benchmark;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * A single item within a benchmark that provides an instruction and workspace for an
 * agent to work on.
 */
public interface BenchmarkTask {

	String id();

	String instruction();

	Path workspaceTemplate();

	Path referenceDir();

	Map<String, Object> metadata();

	/** Optional difficulty tag: easy, medium, hard. Null if unset. */
	String difficulty();

	Duration timeout();

	/** Scripts to run in the workspace before agent invocation. */
	List<String> setup();

	/** Scripts to run in the workspace after agent completes, before grading. */
	List<String> post();

}
