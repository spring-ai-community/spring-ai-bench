package org.springaicommunity.bench.core.result;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Aggregate result of running a complete benchmark.
 */
public record BenchmarkResult(String benchmarkName, String benchmarkVersion, String runId, String agentName,
		List<ItemResult> items, double accuracy, Map<String, Object> aggregateScores, Duration totalDuration,
		double cost) {

	public BenchmarkResult {
		items = List.copyOf(items);
		aggregateScores = aggregateScores != null ? Map.copyOf(aggregateScores) : Map.of();
	}

	public static BenchmarkResult fromItems(String benchmarkName, String benchmarkVersion, String runId,
			String agentName, List<ItemResult> items, Duration totalDuration, double cost) {
		long resolved = items.stream().filter(ItemResult::resolved).count();
		double accuracy = items.isEmpty() ? 0.0 : (double) resolved / items.size();
		return new BenchmarkResult(benchmarkName, benchmarkVersion, runId, agentName, items, accuracy, Map.of(),
				totalDuration, cost);
	}

}
