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

import org.springaicommunity.agents.judge.result.Check;
import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Generates JSON and HTML reports using Judge framework's Judgment.
 */
public class ReportGenerator {

	private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
		.withZone(ZoneOffset.UTC);

	private final ObjectMapper objectMapper = new ObjectMapper();

	public void generateReports(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs, String status,
			Judgment judgment, BenchRunner.JBangResult agentResult) throws IOException {

		// Generate JSON report
		generateJsonReport(runSpec, startedAt, finishedAt, durationMs, status, judgment, agentResult);

		// Generate HTML report
		generateHtmlReport(runSpec, startedAt, finishedAt, durationMs, status, judgment, agentResult);
	}

	private void generateJsonReport(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs,
			String status, Judgment judgment, BenchRunner.JBangResult agentResult) throws IOException {

		Map<String, Object> report = new HashMap<>();
		report.put("schemaVersion", "0.2");
		report.put("runId", runSpec.getRunId());
		report.put("case", runSpec.getCaseId());
		report.put("status", status);
		report.put("startedAtUtc", UTC_FORMATTER.format(startedAt));
		report.put("finishedAtUtc", UTC_FORMATTER.format(finishedAt));
		report.put("durationMs", durationMs);

		// Agent metadata
		Map<String, Object> agentMetadata = new HashMap<>();
		agentMetadata.put("agentType", runSpec.getAgent().getAlias());
		// Extract provider from agent alias (e.g., "hello-world",
		// "hello-world-ai-gemini",
		// "hello-world-ai-claude")
		String provider = extractProvider(runSpec.getAgent().getAlias());
		agentMetadata.put("provider", provider);
		agentMetadata.put("version", "0.1.0-SNAPSHOT");
		report.put("agentMetadata", agentMetadata);

		// Execution metrics
		Map<String, Object> executionMetrics = new HashMap<>();
		executionMetrics.put("durationMs", durationMs);
		// Token metrics - placeholder for now, will be populated from agent result
		Map<String, Object> tokens = new HashMap<>();
		tokens.put("input", 0);
		tokens.put("output", 0);
		tokens.put("total", 0);
		executionMetrics.put("tokens", tokens);
		report.put("executionMetrics", executionMetrics);

		// Checks from Judge framework
		List<Map<String, Object>> checks = new ArrayList<>();
		if (judgment.checks() != null) {
			for (Check check : judgment.checks()) {
				Map<String, Object> checkMap = new HashMap<>();
				checkMap.put("name", check.name());
				checkMap.put("status", check.passed() ? "pass" : "fail");
				checkMap.put("details", check.message());
				checks.add(checkMap);
			}
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

		// Reproduce command (JBang)
		Map<String, Object> reproduce = new HashMap<>();
		reproduce.put("jbangCommand", agentResult.getCommand());
		report.put("reproduce", reproduce);

		// Provenance
		Map<String, Object> provenance = new HashMap<>();
		provenance.put("benchVersion", "0.1.0-SNAPSHOT");
		provenance.put("agentsVersion", "0.1.0-SNAPSHOT");
		provenance.put("judge", "spring-ai-agents-judge");
		provenance.put("generator", "Spring AI Bench");
		provenance.put("reportFormat", "0.2");
		provenance.put("generatedAt", UTC_FORMATTER.format(Instant.now()));
		report.put("provenance", provenance);

		// Write JSON report
		Path reportFile = runSpec.getOutputDir().resolve("report.json");
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), report);
	}

	private String extractProvider(String agentAlias) {
		// Extract provider from agent alias
		// "hello-world" -> "deterministic"
		// "hello-world-ai-gemini" -> "gemini"
		// "hello-world-ai-claude" -> "claude"
		if (agentAlias.contains("-gemini")) {
			return "gemini";
		}
		else if (agentAlias.contains("-claude")) {
			return "claude";
		}
		else if (agentAlias.contains("-ai")) {
			return "ai-unknown";
		}
		else {
			return "deterministic";
		}
	}

	private String escapeHtml(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	private void generateHtmlReport(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs,
			String status, Judgment judgment, BenchRunner.JBangResult agentResult) throws IOException {

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n");
		html.append("<html lang=\"en\">\n");
		html.append("<head>\n");
		html.append("  <meta charset=\"UTF-8\">\n");
		html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
		html.append("  <title>Benchmark Report - ").append(runSpec.getCaseId()).append("</title>\n");
		html.append("  <style>\n");
		html.append(
				"    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; margin: 40px; background: #f9fafb; }\n");
		html.append(
				"    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
		html.append("    .header { border-bottom: 2px solid #333; padding-bottom: 20px; margin-bottom: 30px; }\n");
		html.append("    .header h1 { margin: 0; font-size: 32px; }\n");
		html.append("    .status-success { color: #22c55e; font-weight: bold; font-size: 18px; }\n");
		html.append("    .status-failure { color: #ef4444; font-weight: bold; font-size: 18px; }\n");
		html.append(
				"    .card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 24px; margin: 20px 0; background: #fafafa; }\n");
		html.append(
				"    .card h3 { margin-top: 0; color: #374151; font-size: 20px; border-bottom: 1px solid #e5e7eb; padding-bottom: 10px; }\n");
		html.append("    .check-pass { color: #22c55e; font-weight: 500; }\n");
		html.append("    .check-fail { color: #ef4444; font-weight: 500; }\n");
		html.append(
				"    .monospace { font-family: 'Monaco', 'Menlo', monospace; background: #f3f4f6; padding: 12px; border-radius: 6px; border: 1px solid #d1d5db; font-size: 13px; overflow-x: auto; }\n");
		html.append(
				"    .badge { display: inline-block; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; margin-right: 8px; }\n");
		html.append("    .badge-provider { background: #dbeafe; color: #1e40af; }\n");
		html.append("    .badge-deterministic { background: #e0e7ff; color: #4338ca; }\n");
		html.append("    .badge-ai { background: #fce7f3; color: #9f1239; }\n");
		html.append("    .metric { display: inline-block; margin-right: 30px; }\n");
		html.append("    .metric-label { color: #6b7280; font-size: 14px; }\n");
		html.append("    .metric-value { font-size: 24px; font-weight: 700; color: #111827; }\n");
		html.append(
				"    .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }\n");
		html.append("  </style>\n");
		html.append("</head>\n");
		html.append("<body>\n");
		html.append("  <div class=\"container\">\n");

		// Header
		html.append("  <div class=\"header\">\n");
		html.append("    <h1>Benchmark Report</h1>\n");
		html.append("    <p><strong>Case:</strong> ").append(runSpec.getCaseId()).append("</p>\n");
		html.append("    <p><strong>Run ID:</strong> <code>").append(runSpec.getRunId()).append("</code></p>\n");
		html.append("    <p><strong>Status:</strong> <span class=\"status-")
			.append(status)
			.append("\">")
			.append(status.toUpperCase())
			.append("</span></p>\n");
		html.append("  </div>\n");

		// Agent Info Card
		String provider = extractProvider(runSpec.getAgent().getAlias());
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>🤖 Agent Information</h3>\n");
		html.append("    <p><strong>Agent Type:</strong> ").append(runSpec.getAgent().getAlias()).append("</p>\n");
		html.append("    <p><strong>Provider:</strong> ");
		if ("deterministic".equals(provider)) {
			html.append("<span class=\"badge badge-deterministic\">Deterministic</span>");
		}
		else if ("gemini".equals(provider) || "claude".equals(provider)) {
			html.append("<span class=\"badge badge-ai\">").append(provider.toUpperCase()).append("</span>");
		}
		else {
			html.append("<span class=\"badge badge-provider\">").append(provider).append("</span>");
		}
		html.append("</p>\n");
		html.append("    <p><strong>Version:</strong> 0.1.0-SNAPSHOT</p>\n");
		html.append("  </div>\n");

		// Performance Metrics Card
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>⚡ Performance Metrics</h3>\n");
		html.append("    <div class=\"metrics-grid\">\n");
		html.append("      <div class=\"metric\">\n");
		html.append("        <div class=\"metric-label\">Duration</div>\n");
		html.append("        <div class=\"metric-value\">").append(durationMs).append(" ms</div>\n");
		html.append("      </div>\n");
		html.append("      <div class=\"metric\">\n");
		html.append("        <div class=\"metric-label\">Tokens</div>\n");
		html.append("        <div class=\"metric-value\">0</div>\n");
		html.append("        <div class=\"metric-label\">↑0 ↓0</div>\n");
		html.append("      </div>\n");
		html.append("    </div>\n");
		html.append("  </div>\n");

		// Execution Summary
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>📊 Execution Summary</h3>\n");
		html.append("    <p><strong>Started (UTC):</strong> ").append(UTC_FORMATTER.format(startedAt)).append("</p>\n");
		html.append("    <p><strong>Finished (UTC):</strong> ")
			.append(UTC_FORMATTER.format(finishedAt))
			.append("</p>\n");
		html.append("    <p><strong>Description:</strong> ").append(runSpec.getDescription()).append("</p>\n");
		html.append("  </div>\n");

		// Judge Results
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>✅ Judge Results</h3>\n");
		if (judgment.checks() != null) {
			for (Check check : judgment.checks()) {
				String checkClass = check.passed() ? "check-pass" : "check-fail";
				String checkStatus = check.passed() ? "✅ PASS" : "❌ FAIL";
				html.append("    <p><span class=\"")
					.append(checkClass)
					.append("\">")
					.append(checkStatus)
					.append("</span> <strong>")
					.append(check.name())
					.append(":</strong> ")
					.append(check.message())
					.append("</p>\n");
			}
		}
		html.append("  </div>\n");

		// Artifacts
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>📦 Artifacts</h3>\n");
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

		// Reproduce It Now
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>🔄 Reproduce It Now</h3>\n");
		html.append("    <p><strong>Exact JBang command:</strong></p>\n");
		html.append("    <div class=\"monospace\">").append(escapeHtml(agentResult.getCommand())).append("</div>\n");
		html.append("    <p style=\"margin-top: 15px;\"><em>Copy and paste the command above to reproduce this exact "
				+ "benchmark run.</em></p>\n");
		html.append("  </div>\n");

		// Machine-Readable
		html.append("  <div class=\"card\">\n");
		html.append("    <h3>📄 Machine-Readable Data</h3>\n");
		html.append("    <p><a href=\"report.json\">📊 report.json</a> - Complete structured report data (JSON)</p>\n");
		html.append("    <p><a href=\"logs.txt\">📝 logs.txt</a> - Detailed execution logs</p>\n");
		html.append("  </div>\n");

		html.append("  </div>\n"); // Close container
		html.append("</body>\n");
		html.append("</html>\n");

		// Write HTML report
		Path htmlFile = runSpec.getOutputDir().resolve("report.html");
		Files.write(htmlFile, html.toString().getBytes());
	}

}