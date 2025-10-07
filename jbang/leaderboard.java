///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2

package org.springaicommunity.bench;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class leaderboard {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static class AgentStats {

		String agentType;

		String provider;

		String model;

		int totalRuns;

		int successfulRuns;

		int failedRuns;

		double successRate;

		long meanDurationMs;

		long medianDurationMs;

		long minDurationMs;

		long maxDurationMs;

		List<Long> durations = new ArrayList<>();

		List<String> runIds = new ArrayList<>();

		public void addRun(long duration, boolean success, String runId) {
			totalRuns++;
			if (success) {
				successfulRuns++;
			}
			else {
				failedRuns++;
			}
			durations.add(duration);
			runIds.add(runId);
			calculateStats();
		}

		private void calculateStats() {
			successRate = totalRuns > 0 ? (double) successfulRuns / totalRuns * 100.0 : 0.0;

			if (!durations.isEmpty()) {
				meanDurationMs = (long) durations.stream().mapToLong(Long::longValue).average().orElse(0);
				Collections.sort(durations);
				int middle = durations.size() / 2;
				medianDurationMs = durations.size() % 2 == 0
						? (durations.get(middle - 1) + durations.get(middle)) / 2 : durations.get(middle);
				minDurationMs = durations.stream().mapToLong(Long::longValue).min().orElse(0);
				maxDurationMs = durations.stream().mapToLong(Long::longValue).max().orElse(0);
			}
		}

		public double getStdDev() {
			if (durations.size() < 2)
				return 0.0;
			double mean = meanDurationMs;
			double variance = durations.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0);
			return Math.sqrt(variance);
		}

	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Usage: jbang leaderboard.java <reports-dir> [output-file]");
			System.exit(1);
		}

		String reportsDir = args[0];
		String outputFile = args.length > 1 ? args[1] : reportsDir + "/leaderboard.html";

		generateLeaderboard(reportsDir, outputFile);

		System.out.println("✅ Leaderboard generated: " + outputFile);
	}

	public static void generateLeaderboard(String reportsDir, String outputFile) throws IOException {
		Path reportsPath = Paths.get(reportsDir);
		Map<String, AgentStats> agentStatsMap = new HashMap<>();

		// Scan all report.json files
		Files.list(reportsPath).filter(Files::isDirectory).forEach(dir -> {
			try {
				Path reportFile = dir.resolve("report.json");
				if (Files.exists(reportFile)) {
					JsonNode report = objectMapper.readTree(reportFile.toFile());

					String agentType = report.path("agentMetadata").path("agentType").asText(null);
					String provider = report.path("agentMetadata").path("provider").asText(null);
					String model = report.path("model").path("name").asText("N/A");
					long duration = report.path("durationMs").asLong(0);
					boolean success = "success".equals(report.path("status").asText(""));
					String runId = report.path("runId").asText(dir.getFileName().toString());

					// Skip old schema reports
					if (agentType == null || provider == null) {
						return;
					}

					String key = agentType + "-" + provider;
					AgentStats stats = agentStatsMap.computeIfAbsent(key, k -> {
						AgentStats s = new AgentStats();
						s.agentType = agentType;
						s.provider = provider;
						s.model = model;
						return s;
					});
					stats.addRun(duration, success, runId);
				}
			}
			catch (IOException e) {
				// Skip invalid reports
			}
		});

		System.out.println("Found " + agentStatsMap.size() + " agent(s) across " +
				agentStatsMap.values().stream().mapToInt(s -> s.totalRuns).sum() + " run(s)");

		// Generate HTML
		generateHtml(agentStatsMap, outputFile);
	}

	private static void generateHtml(Map<String, AgentStats> agentStatsMap, String outputFile) throws IOException {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"en\">\n");
		html.append("<head>\n");
		html.append("  <meta charset=\"UTF-8\">\n");
		html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("  <title>Spring AI Bench Leaderboard</title>\n");
		html.append("  <style>\n");
		html.append(
				"    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 40px; background: #f9fafb; }\n");
		html.append(
				"    .container { max-width: 1400px; margin: 0 auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
		html.append("    h1 { font-size: 36px; margin-bottom: 10px; }\n");
		html.append("    .subtitle { color: #6b7280; font-size: 16px; margin-bottom: 30px; }\n");
		html.append("    table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
		html.append(
				"    th, td { padding: 16px; text-align: left; border-bottom: 1px solid #e5e7eb; }\n");
		html.append("    th { background: #f9fafb; font-weight: 600; color: #374151; }\n");
		html.append("    tr:hover { background: #f9fafb; }\n");
		html.append("    .rank { font-size: 24px; font-weight: 700; }\n");
		html.append("    .badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; margin-right: 8px; }\n");
		html.append("    .badge-deterministic { background: #e0e7ff; color: #4338ca; }\n");
		html.append("    .badge-ai { background: #fce7f3; color: #9f1239; }\n");
		html.append("    .success-rate-high { color: #22c55e; font-weight: 600; }\n");
		html.append("    .success-rate-med { color: #f59e0b; font-weight: 600; }\n");
		html.append("    .success-rate-low { color: #ef4444; font-weight: 600; }\n");
		html.append("    .metric-value { font-weight: 600; }\n");
		html.append("  </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("  <div class=\"container\">\n");
		html.append("    <h1>🏆 Spring AI Bench Leaderboard</h1>\n");
		html.append("    <p class=\"subtitle\">Benchmarking AI developer agents on enterprise Java workflows</p>\n");

		// Sort agents by mean duration (fastest first)
		List<AgentStats> sortedAgents = agentStatsMap.values()
			.stream()
			.sorted(Comparator.comparingLong(a -> a.meanDurationMs))
			.collect(Collectors.toList());

		html.append("    <table>\n");
		html.append("      <thead>\n");
		html.append("        <tr>\n");
		html.append("          <th>Rank</th>\n");
		html.append("          <th>Agent</th>\n");
		html.append("          <th>Provider</th>\n");
		html.append("          <th>Model</th>\n");
		html.append("          <th>Success Rate</th>\n");
		html.append("          <th>Mean Duration</th>\n");
		html.append("          <th>Median Duration</th>\n");
		html.append("          <th>Runs</th>\n");
		html.append("        </tr>\n");
		html.append("      </thead>\n");
		html.append("      <tbody>\n");

		int rank = 1;
		for (AgentStats stats : sortedAgents) {
			// Make entire row clickable to first run
			String firstRunId = stats.runIds.isEmpty() ? "" : stats.runIds.get(0);
			String rowClass = firstRunId.isEmpty() ? "" : " style=\"cursor: pointer;\" onclick=\"window.location='" + firstRunId + "/report.html'\"";

			html.append("        <tr" + rowClass + ">\n");

			// Rank with medals
			html.append("          <td class=\"rank\">");
			if (rank == 1)
				html.append("🥇");
			else if (rank == 2)
				html.append("🥈");
			else if (rank == 3)
				html.append("🥉");
			else
				html.append(rank);
			html.append("</td>\n");

			// Agent Type - clickable
			html.append("          <td><strong>");
			if (!firstRunId.isEmpty()) {
				html.append("<a href=\"" + firstRunId + "/report.html\" style=\"color: inherit; text-decoration: none;\">");
			}
			html.append(stats.agentType);
			if (!firstRunId.isEmpty()) {
				html.append("</a>");
			}
			html.append("</strong></td>\n");

			// Provider Badge
			html.append("          <td>");
			if ("deterministic".equals(stats.provider)) {
				html.append("<span class=\"badge badge-deterministic\">DETERMINISTIC</span>");
			}
			else {
				html.append("<span class=\"badge badge-ai\">").append(stats.provider.toUpperCase()).append("</span>");
			}
			html.append("</td>\n");

			// Model
			html.append("          <td>").append(stats.model).append("</td>\n");

			// Success Rate
			html.append("          <td><span class=\"");
			if (stats.successRate >= 90)
				html.append("success-rate-high");
			else if (stats.successRate >= 50)
				html.append("success-rate-med");
			else
				html.append("success-rate-low");
			html.append("\">");
			html.append(String.format("%.1f%%", stats.successRate));
			html.append("</span></td>\n");

			// Mean Duration
			html.append("          <td class=\"metric-value\">").append(formatDuration(stats.meanDurationMs));
			if (stats.durations.size() > 1) {
				html.append(" <span style=\"color: #6b7280; font-weight: normal;\">±")
					.append(String.format("%.0f ms", stats.getStdDev()))
					.append("</span>");
			}
			html.append("</td>\n");

			// Median Duration
			html.append("          <td>").append(formatDuration(stats.medianDurationMs)).append("</td>\n");

			// Runs
			html.append("          <td>")
				.append(stats.totalRuns)
				.append(" (")
				.append(stats.successfulRuns)
				.append("✓ ")
				.append(stats.failedRuns)
				.append("✗)")
				.append("</td>\n");

			html.append("        </tr>\n");
			rank++;
		}

		html.append("      </tbody>\n");
		html.append("    </table>\n");

		// Footer
		html.append("    <p style=\"margin-top: 40px; color: #6b7280; font-size: 14px;\">\n");
		html.append("      Generated by Spring AI Bench • <a href=\"index.html\">View All Reports</a>\n");
		html.append("    </p>\n");

		html.append("  </div>\n");
		html.append("</body>\n");
		html.append("</html>\n");

		// Write HTML file
		Files.write(Paths.get(outputFile), html.toString().getBytes());
	}

	private static String formatDuration(long ms) {
		if (ms < 1000) {
			return ms + " ms";
		}
		else if (ms < 60000) {
			return String.format("%.1f s", ms / 1000.0);
		}
		else {
			long minutes = ms / 60000;
			long seconds = (ms % 60000) / 1000;
			return String.format("%d:%02d", minutes, seconds);
		}
	}

}
