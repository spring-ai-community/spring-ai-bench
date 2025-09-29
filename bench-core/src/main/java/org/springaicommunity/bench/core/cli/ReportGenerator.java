package org.springaicommunity.bench.core.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates JSON and HTML reports according to the genesis plan.
 */
public class ReportGenerator {

	private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
		.withZone(ZoneOffset.UTC);

	private final ObjectMapper objectMapper = new ObjectMapper();

	public void generateReports(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs, String status,
			BenchRunner.VerificationResult verificationResult, BenchRunner.JBangResult agentResult) throws IOException {

		// Generate JSON report
		generateJsonReport(runSpec, startedAt, finishedAt, durationMs, status, verificationResult, agentResult);

		// Generate HTML report
		generateHtmlReport(runSpec, startedAt, finishedAt, durationMs, status, verificationResult, agentResult);
	}

	private void generateJsonReport(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs,
			String status, BenchRunner.VerificationResult verificationResult, BenchRunner.JBangResult agentResult)
			throws IOException {

		Map<String, Object> report = new HashMap<>();
		report.put("schemaVersion", "0.1");
		report.put("runId", runSpec.getRunId());
		report.put("case", runSpec.getCaseId());
		report.put("status", status);
		report.put("startedAtUtc", UTC_FORMATTER.format(startedAt));
		report.put("finishedAtUtc", UTC_FORMATTER.format(finishedAt));
		report.put("durationMs", durationMs);

		// Checks
		List<Map<String, Object>> checks = new ArrayList<>();
		for (BenchRunner.CheckResult checkResult : verificationResult.getCheckResults()) {
			Map<String, Object> check = new HashMap<>();
			check.put("name", checkResult.getName());
			check.put("status", checkResult.isPass() ? "pass" : "fail");
			check.put("details", checkResult.getDetails());
			checks.add(check);
		}
		report.put("checks", checks);

		// Artifacts
		Map<String, Object> artifacts = new HashMap<>();
		artifacts.put("workspaceRel", "workspace/");
		List<String> outputs = new ArrayList<>();
		// Find outputs in workspace
		Path workspace = runSpec.getOutputDir().resolve("workspace");
		if (Files.exists(workspace)) {
			Files.walk(workspace).filter(Files::isRegularFile).forEach(file -> {
				String relativePath = workspace.relativize(file).toString().replace('\\', '/');
				outputs.add("workspace/" + relativePath);
			});
		}
		artifacts.put("outputs", outputs);
		report.put("artifacts", artifacts);

		// Reproduce command
		Map<String, Object> reproduce = new HashMap<>();
		reproduce.put("command", agentResult.getCommand());
		report.put("reproduce", reproduce);

		// Provenance
		Map<String, Object> provenance = new HashMap<>();
		provenance.put("benchVersion", "0.0.1");
		provenance.put("agent", runSpec.getAgent().getAlias());
		provenance.put("verifier", "exists+stringEquals");
		provenance.put("reporter", "html+json");
		report.put("provenance", provenance);

		// Write JSON report
		Path reportFile = runSpec.getOutputDir().resolve("report.json");
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), report);
	}

	private void generateHtmlReport(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs,
			String status, BenchRunner.VerificationResult verificationResult, BenchRunner.JBangResult agentResult)
			throws IOException {

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"en\">\n");
		html.append("<head>\n");
		html.append("  <meta charset=\"UTF-8\">\n");
		html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("  <title>Benchmark Report - ").append(runSpec.getCaseId()).append("</title>\n");
		html.append("  <style>\n");
		html.append(
				"    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 40px; }\n");
		html.append("    .header { border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }\n");
		html.append("    .status-success { color: #22c55e; font-weight: bold; }\n");
		html.append("    .status-failure { color: #ef4444; font-weight: bold; }\n");
		html.append("    .card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px; margin: 20px 0; }\n");
		html.append("    .card h3 { margin-top: 0; color: #374151; }\n");
		html.append("    .check-pass { color: #22c55e; }\n");
		html.append("    .check-fail { color: #ef4444; }\n");
		html.append(
				"    .monospace { font-family: 'Monaco', 'Menlo', monospace; background: #f9fafb; padding: 10px; border-radius: 4px; }\n");
		html.append("  </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");

		// Header
		html.append("  <div class=\"header\">\n");
		html.append("    <h1>Benchmark Report</h1>\n");
		html.append("    <p><strong>Case:</strong> ").append(runSpec.getCaseId()).append("</p>\n");
		html.append("    <p><strong>Run ID:</strong> ").append(runSpec.getRunId()).append("</p>\n");
		html.append("    <p><strong>Status:</strong> <span class=\"status-")
			.append(status)
			.append("\">")
			.append(status.toUpperCase())
			.append("</span></p>\n");
		html.append("  </div>\n");

		// Execution Summary
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>Execution Summary</h3>\n");
		html.append("    <p><strong>Started (UTC):</strong> ").append(UTC_FORMATTER.format(startedAt)).append("</p>\n");
		html.append("    <p><strong>Finished (UTC):</strong> ")
			.append(UTC_FORMATTER.format(finishedAt))
			.append("</p>\n");
		html.append("    <p><strong>Duration:</strong> ").append(durationMs).append(" ms</p>\n");
		html.append("    <p><strong>Description:</strong> ").append(runSpec.getDescription()).append("</p>\n");
		html.append("  </div>\n");

		// Verification Results
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>Verification Results</h3>\n");
		for (BenchRunner.CheckResult checkResult : verificationResult.getCheckResults()) {
			String checkClass = checkResult.isPass() ? "check-pass" : "check-fail";
			String checkStatus = checkResult.isPass() ? "✅ PASS" : "❌ FAIL";
			html.append("    <p><span class=\"")
				.append(checkClass)
				.append("\">")
				.append(checkStatus)
				.append("</span> ")
				.append(checkResult.getName())
				.append(": ")
				.append(checkResult.getDetails())
				.append("</p>\n");
		}
		html.append("  </div>\n");

		// Artifacts
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>Artifacts</h3>\n");
		Path workspace = runSpec.getOutputDir().resolve("workspace");
		if (Files.exists(workspace)) {
			html.append("    <p><strong>Workspace:</strong> <a href=\"workspace/\">workspace/</a></p>\n");
			html.append("    <p><strong>Files created:</strong></p>\n");
			html.append("    <ul>\n");
			Files.walk(workspace).filter(Files::isRegularFile).forEach(file -> {
				String relativePath = workspace.relativize(file).toString().replace('\\', '/');
				html.append("      <li><a href=\"workspace/")
					.append(relativePath)
					.append("\">")
					.append(relativePath)
					.append("</a></li>\n");
			});
			html.append("    </ul>\n");
		}
		else {
			html.append("    <p>No artifacts created.</p>\n");
		}
		html.append("  </div>\n");

		// Machine-Readable
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>Machine-Readable</h3>\n");
		html.append("    <p><a href=\"report.json\">report.json</a> - Full report data</p>\n");
		html.append("    <p><a href=\"logs.txt\">logs.txt</a> - Execution logs</p>\n");
		html.append("  </div>\n");

		// Reproduce It Now
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>Reproduce It Now</h3>\n");
		html.append("    <p><strong>Exact JBang command:</strong></p>\n");
		html.append("    <div class=\"monospace\">").append(agentResult.getCommand()).append("</div>\n");
		html.append("    <p><strong>Or use bench CLI:</strong></p>\n");
		html.append("    <div class=\"monospace\">bench run --run-file runs/examples/hello-world-run.yaml</div>\n");
		html.append("  </div>\n");

		html.append("</body>\n");
		html.append("</html>\n");

		// Write HTML report
		Path htmlFile = runSpec.getOutputDir().resolve("report.html");
		Files.write(htmlFile, html.toString().getBytes());
	}

}