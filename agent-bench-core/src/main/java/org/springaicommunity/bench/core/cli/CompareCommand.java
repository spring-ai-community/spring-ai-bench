package org.springaicommunity.bench.core.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springaicommunity.bench.core.result.BenchmarkResult;
import org.springaicommunity.bench.core.result.TrialResult;

/**
 * Compares benchmark results across multiple runs. Reads result.json from each run
 * directory and prints a side-by-side comparison table.
 */
public class CompareCommand {

	private final ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@SuppressWarnings("unchecked")
	public void compare(List<Path> runDirs) throws IOException {
		List<BenchmarkResult> results = new ArrayList<>();
		for (Path dir : runDirs) {
			Path resultFile = dir.resolve("result.json");
			if (!Files.isRegularFile(resultFile)) {
				System.err.println("Warning: no result.json in " + dir);
				continue;
			}
			results.add(jsonMapper.readValue(resultFile.toFile(), BenchmarkResult.class));
		}

		if (results.isEmpty()) {
			System.out.println("No results to compare.");
			return;
		}

		// Header
		System.out.printf("%-20s %-10s %-8s", "Agent", "Accuracy", "Pass@1");
		boolean hasMultipleAttempts = results.stream()
			.anyMatch(r -> r.aggregateScores().containsKey("passAtK")
					&& ((Map<?, ?>) r.aggregateScores().get("passAtK")).size() > 1);
		if (hasMultipleAttempts) {
			System.out.printf(" %-8s", "Pass@2");
		}
		System.out.printf(" %-10s %-10s %-8s%n", "Cost", "Duration", "Trials");
		System.out.println("-".repeat(hasMultipleAttempts ? 84 : 74));

		for (BenchmarkResult result : results) {
			String agent = truncate(result.agentName(), 20);
			String accuracy = String.format("%.1f%%", result.accuracy() * 100);

			Map<String, Double> passAtK = Map.of();
			Object raw = result.aggregateScores().get("passAtK");
			if (raw instanceof Map<?, ?> map) {
				passAtK = toDoubleMap(map);
			}

			String pass1 = passAtK.containsKey("1") ? String.format("%.1f%%", passAtK.get("1") * 100) : "-";
			String cost = formatCost(result);
			String duration = formatDuration(result.totalDuration());
			int trials = result.trials().size();

			System.out.printf("%-20s %-10s %-8s", agent, accuracy, pass1);
			if (hasMultipleAttempts) {
				String pass2 = passAtK.containsKey("2") ? String.format("%.1f%%", passAtK.get("2") * 100) : "-";
				System.out.printf(" %-8s", pass2);
			}
			System.out.printf(" %-10s %-10s %-8d%n", cost, duration, trials);
		}
	}

	private String formatCost(BenchmarkResult result) {
		double cost = result.cost();
		if (cost <= 0) {
			// Try to sum from trial scores
			cost = result.trials()
				.stream()
				.map(TrialResult::scores)
				.filter(s -> s.containsKey("costUsd"))
				.mapToDouble(s -> ((Number) s.get("costUsd")).doubleValue())
				.sum();
		}
		return cost > 0 ? String.format("$%.4f", cost) : "-";
	}

	private static String formatDuration(java.time.Duration d) {
		if (d == null) {
			return "-";
		}
		long secs = d.getSeconds();
		if (secs < 60) {
			return secs + "s";
		}
		if (secs < 3600) {
			return String.format("%dm%ds", secs / 60, secs % 60);
		}
		return String.format("%dh%dm", secs / 3600, (secs % 3600) / 60);
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "unknown";
		}
		return s.length() <= max ? s : s.substring(0, max - 3) + "...";
	}

	private static Map<String, Double> toDoubleMap(Map<?, ?> raw) {
		java.util.Map<String, Double> result = new java.util.HashMap<>();
		for (Map.Entry<?, ?> entry : raw.entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (entry.getValue() instanceof Number n) {
				result.put(key, n.doubleValue());
			}
		}
		return result;
	}

}
