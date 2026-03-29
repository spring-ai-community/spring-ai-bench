package org.springaicommunity.bench.core.result;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregate result of running a complete benchmark.
 */
public record BenchmarkResult(String benchmarkName, String benchmarkVersion, String runId, String agentName,
		List<TrialResult> trials, double accuracy, Map<String, Object> aggregateScores, Duration totalDuration,
		double cost) {

	public BenchmarkResult {
		trials = List.copyOf(trials);
		aggregateScores = aggregateScores != null ? Map.copyOf(aggregateScores) : Map.of();
	}

	public static BenchmarkResult fromTrials(String benchmarkName, String benchmarkVersion, String runId,
			String agentName, List<TrialResult> trials, Duration totalDuration, double cost) {
		long resolved = trials.stream().filter(TrialResult::resolved).count();
		double accuracy = trials.isEmpty() ? 0.0 : (double) resolved / trials.size();
		Map<String, Object> scores = new HashMap<>();
		scores.put("passAtK", computePassAtK(trials));
		return new BenchmarkResult(benchmarkName, benchmarkVersion, runId, agentName, trials, accuracy, scores,
				totalDuration, cost);
	}

	/**
	 * Computes pass@k metrics from item results. Groups by taskId (multiple attempts
	 * per item), then for each k computes: 1 - C(n-c, k) / C(n, k) where n = attempts,
	 * c = successes. Formula from terminal-bench / Chen et al. (2021).
	 */
	public static Map<Integer, Double> computePassAtK(List<TrialResult> trials) {
		// Group by taskId
		Map<String, List<TrialResult>> byTask = trials.stream()
			.collect(Collectors.groupingBy(TrialResult::taskId));

		if (byTask.isEmpty()) {
			return Map.of();
		}

		int minAttempts = byTask.values().stream().mapToInt(List::size).min().orElse(1);
		if (minAttempts <= 1) {
			return Map.of(1, trials.isEmpty() ? 0.0
					: (double) trials.stream().filter(TrialResult::resolved).count() / trials.size());
		}

		Map<Integer, Double> passAtK = new HashMap<>();
		// Compute for k = 1 and powers of 2 up to minAttempts
		List<Integer> kValues = new java.util.ArrayList<>();
		kValues.add(1);
		for (int k = 2; k <= minAttempts; k *= 2) {
			kValues.add(k);
		}

		for (int k : kValues) {
			double sum = 0.0;
			int eligible = 0;
			for (List<TrialResult> attempts : byTask.values()) {
				if (attempts.size() < k) {
					continue;
				}
				int n = attempts.size();
				int c = (int) attempts.stream().filter(TrialResult::resolved).count();
				// pass@k = 1 - C(n-c, k) / C(n, k)
				double passK = 1.0 - binomialCoeff(n - c, k) / binomialCoeff(n, k);
				sum += passK;
				eligible++;
			}
			passAtK.put(k, eligible > 0 ? sum / eligible : 0.0);
		}
		return Map.copyOf(passAtK);
	}

	private static double binomialCoeff(int n, int k) {
		if (k > n || k < 0) {
			return 0.0;
		}
		if (k == 0 || k == n) {
			return 1.0;
		}
		// Use logarithms to avoid overflow
		double result = 0.0;
		for (int i = 0; i < k; i++) {
			result += Math.log(n - i) - Math.log(i + 1);
		}
		return Math.exp(result);
	}

}
