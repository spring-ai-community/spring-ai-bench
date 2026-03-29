package org.springaicommunity.bench.core.benchmark;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * A benchmark consisting of one or more items to evaluate an agent against.
 */
public interface Benchmark {

	String name();

	String version();

	Map<String, Object> metadata();

	List<BenchmarkTask> tasks();

	Map<String, Object> juryConfig();

	Duration defaultTimeout();

}
