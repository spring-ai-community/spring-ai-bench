/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.bench.agents.report;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.bench.agents.verifier.Check;
import org.springaicommunity.bench.agents.verifier.VerificationResult;

/**
 * Generates minimal HTML reports for agent execution results. Shows verification checks
 * with clear pass/fail indicators.
 */
public class MinimalHtmlReportGenerator {

	public static Path generate(UUID runId, String caseId, boolean success, Instant startedAt, Instant finishedAt,
			long durationMs, VerificationResult verificationResult, Path runRoot, AgentResponse agentResponse)
			throws Exception {

		String checksHtml = generateChecksHtml(verificationResult);
		String artifactsHtml = generateArtifactsHtml(runRoot);
		String logTailHtml = generateLogTailHtml(runRoot);
		String agentDetailsHtml = generateAgentDetailsHtml(agentResponse);

		String html = """
				<!DOCTYPE html>
				<html>
				<head>
				    <meta charset="UTF-8">
				    <title>Agent Run Report: %s</title>
				    <style>
				        body {
				            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
				            max-width: 1000px;
				            margin: 0 auto;
				            padding: 20px;
				            line-height: 1.6;
				            background: #f8fafc;
				        }
				        .header {
				            background: %s;
				            color: white;
				            padding: 30px;
				            border-radius: 12px;
				            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
				            margin-bottom: 30px;
				        }
				        .header h1 {
				            margin: 0 0 10px 0;
				            font-size: 2.2em;
				        }
				        .status {
				            font-size: 1.4em;
				            font-weight: bold;
				            margin-top: 15px;
				        }
				        .info-card {
				            background: white;
				            padding: 25px;
				            border-radius: 8px;
				            margin-bottom: 20px;
				            box-shadow: 0 2px 4px rgba(0,0,0,0.05);
				            border-left: 4px solid %s;
				        }
				        .info-grid {
				            display: grid;
				            grid-template-columns: 150px 1fr;
				            gap: 15px;
				            margin: 20px 0;
				        }
				        .label {
				            font-weight: 600;
				            color: #374151;
				        }
				        .value {
				            color: #111827;
				            font-family: 'SF Mono', Monaco, monospace;
				        }
				        .checks-table {
				            width: 100%%;
				            border-collapse: collapse;
				            margin: 20px 0;
				        }
				        .checks-table th, .checks-table td {
				            padding: 12px 15px;
				            text-align: left;
				            border-bottom: 1px solid #e5e7eb;
				        }
				        .checks-table th {
				            background: #f9fafb;
				            font-weight: 600;
				            color: #374151;
				        }
				        .check-pass { color: #16a34a; font-weight: bold; }
				        .check-fail { color: #dc2626; font-weight: bold; }
				        .log-container {
				            background: #1f2937;
				            color: #f9fafb;
				            padding: 20px;
				            border-radius: 8px;
				            overflow-x: auto;
				            font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
				            font-size: 0.9em;
				            line-height: 1.4;
				            max-height: 400px;
				            overflow-y: auto;
				        }
				        .section h2 {
				            color: #374151;
				            margin-bottom: 15px;
				            font-size: 1.3em;
				        }
				    </style>
				</head>
				<body>
				    <div class="header">
				        <h1>🤖 Agent Execution Report</h1>
				        <div>Run ID: %s</div>
				        <div>Case: %s</div>
				        <div class="status">%s %s</div>
				    </div>

				    <div class="info-card">
				        <h2>📊 Execution Summary</h2>
				        <div class="info-grid">
				            <div class="label">Started:</div>
				            <div class="value">%s</div>

				            <div class="label">Finished:</div>
				            <div class="value">%s</div>

				            <div class="label">Duration:</div>
				            <div class="value">%d ms</div>

				            <div class="label">Result:</div>
				            <div class="value">%s</div>
				        </div>
				    </div>

				    <div class="info-card">
				        <h2>🤖 Agent Details</h2>
				        %s
				    </div>

				    <div class="info-card">
				        <h2>✅ Verification Checks</h2>
				        %s
				    </div>

				    <div class="info-card">
				        <h2>📁 Generated Artifacts</h2>
				        %s
				    </div>

				    <div class="info-card">
				        <h2>📝 Execution Log</h2>
				        %s
				    </div>

				    <div style="text-align: center; margin-top: 40px; color: #6b7280; font-size: 0.9em;">
				        Generated by Spring AI Bench • %s
				    </div>
				</body>
				</html>
				""".formatted(runId, // title
				success ? "linear-gradient(135deg, #10b981 0%, #059669 100%)"
						: "linear-gradient(135deg, #ef4444 0%, #dc2626 100%)", // header
																				// color
				success ? "#10b981" : "#ef4444", // border color
				runId, // run ID
				caseId, // case ID
				success ? "✅" : "❌", // status icon
				success ? "SUCCESS" : "FAILED", // status text
				DateTimeFormatter.ISO_INSTANT.format(startedAt), // started UTC
				DateTimeFormatter.ISO_INSTANT.format(finishedAt), // finished UTC
				durationMs, // duration
				verificationResult != null ? verificationResult.reason() : "No verification", agentDetailsHtml, // agent
																												// details
																												// section
				checksHtml, // checks table
				artifactsHtml, // artifacts section
				logTailHtml, // log content
				DateTimeFormatter.ISO_INSTANT.format(Instant.now()) // generated at UTC
		);

		Path htmlFile = runRoot.resolve("index.html");
		Files.writeString(htmlFile, html);
		return htmlFile;
	}

	private static String generateChecksHtml(VerificationResult verificationResult) {
		if (verificationResult == null || verificationResult.checks().isEmpty()) {
			return "<p>No verification checks performed.</p>";
		}

		StringBuilder html = new StringBuilder();
		html.append("<table class=\"checks-table\">");
		html.append("<thead><tr><th>Check</th><th>Status</th><th>Details</th></tr></thead>");
		html.append("<tbody>");

		for (Check check : verificationResult.checks()) {
			String statusClass = check.pass() ? "check-pass" : "check-fail";
			String statusText = check.pass() ? "✅ PASS" : "❌ FAIL";

			html.append("<tr>");
			html.append("<td>").append(check.name()).append("</td>");
			html.append("<td class=\"").append(statusClass).append("\">").append(statusText).append("</td>");
			html.append("<td>").append(check.info()).append("</td>");
			html.append("</tr>");
		}

		html.append("</tbody></table>");
		return html.toString();
	}

	private static String generateArtifactsHtml(Path runRoot) {
		try {
			// Look for workspace directory and check for hello.txt
			Path workspaceDir = null;

			// Find workspace directory (could be under runRoot or relative)
			if (Files.exists(runRoot.getParent()) && Files.isDirectory(runRoot.getParent())) {
				// Look for directories that might contain hello.txt
				Path possibleWorkspace = Files.list(runRoot.getParent())
					.filter(Files::isDirectory)
					.filter(dir -> !dir.equals(runRoot)) // Exclude the run directory
															// itself
					.filter(dir -> Files.exists(dir.resolve("hello.txt")))
					.findFirst()
					.orElse(null);

				if (possibleWorkspace != null) {
					workspaceDir = possibleWorkspace;
				}
			}

			if (workspaceDir != null && Files.exists(workspaceDir.resolve("hello.txt"))) {
				Path helloFile = workspaceDir.resolve("hello.txt");
				String relativePath = runRoot.getParent().relativize(helloFile).toString();
				long fileSize = Files.size(helloFile);

				return String.format("""
						<table class="checks-table">
						    <thead><tr><th>File</th><th>Size</th><th>Action</th></tr></thead>
						    <tbody>
						        <tr>
						            <td>hello.txt</td>
						            <td>%d bytes</td>
						            <td><a href="../%s" target="_blank">📄 View</a></td>
						        </tr>
						    </tbody>
						</table>
						""", fileSize, relativePath);
			}
			else {
				return "<p>No artifacts found in workspace.</p>";
			}
		}
		catch (Exception e) {
			return "<p>Error scanning for artifacts: " + e.getMessage() + "</p>";
		}
	}

	private static String generateAgentDetailsHtml(AgentResponse agentResponse) {
		if (agentResponse == null) {
			return "<p>No agent response metadata available.</p>";
		}

		try {
			StringBuilder html = new StringBuilder();
			html.append("<div class=\"info-grid\">");

			// Extract basic metadata
			var metadata = agentResponse.getMetadata();
			if (metadata != null) {
				// Model information
				String model = metadata.getModel();
				if (model != null) {
					html.append("<div class=\"label\">Model:</div>");
					html.append("<div class=\"value\">").append(model).append("</div>");
				}

				// Session ID
				String sessionId = metadata.getSessionId();
				if (sessionId != null) {
					html.append("<div class=\"label\">Session ID:</div>");
					html.append("<div class=\"value\">").append(sessionId).append("</div>");
				}

				// Agent duration (different from overall execution duration)
				Object agentDuration = metadata.get("duration");
				if (agentDuration != null) {
					html.append("<div class=\"label\">Agent Duration:</div>");
					html.append("<div class=\"value\">").append(agentDuration).append("</div>");
				}

				// Extract Claude-specific metadata
				Object claudeMetadata = metadata.get("claude_metadata");
				if (claudeMetadata != null) {
					String claudeStr = claudeMetadata.toString();

					// Parse cost information
					String totalCost = extractCostFromString(claudeStr);
					if (totalCost != null) {
						html.append("<div class=\"label\">Total Cost:</div>");
						html.append("<div class=\"value\">$").append(totalCost).append("</div>");
					}

					// Parse token usage
					String inputTokens = extractValueFromString(claudeStr, "inputTokens=(\\d+)");
					if (inputTokens != null) {
						html.append("<div class=\"label\">Input Tokens:</div>");
						html.append("<div class=\"value\">").append(inputTokens).append("</div>");
					}

					String outputTokens = extractValueFromString(claudeStr, "outputTokens=(\\d+)");
					if (outputTokens != null) {
						html.append("<div class=\"label\">Output Tokens:</div>");
						html.append("<div class=\"value\">").append(outputTokens).append("</div>");
					}

					// Parse agent duration
					String agentDurationMs = extractValueFromString(claudeStr, "durationMs=(\\d+)");
					if (agentDurationMs != null) {
						html.append("<div class=\"label\">Agent Duration:</div>");
						html.append("<div class=\"value\">").append(agentDurationMs).append(" ms</div>");
					}

					// Parse actual model used
					String actualModel = extractValueFromString(claudeStr, "model=([^,\\]]+)");
					if (actualModel != null && !actualModel.equals(model)) {
						html.append("<div class=\"label\">Actual Model:</div>");
						html.append("<div class=\"value\">").append(actualModel).append("</div>");
					}

					// Parse number of turns
					String numTurns = extractValueFromString(claudeStr, "numTurns=(\\d+)");
					if (numTurns != null) {
						html.append("<div class=\"label\">Conversation Turns:</div>");
						html.append("<div class=\"value\">").append(numTurns).append("</div>");
					}
				}

				// Show all available metadata for debugging
				html.append("<div class=\"label\">Available Metadata:</div>");
				html.append("<div class=\"value\">");
				for (Map.Entry<String, Object> entry : metadata.entrySet()) {
					html.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>");
				}
				html.append("</div>");
			}

			html.append("</div>");
			return html.toString();

		}
		catch (Exception e) {
			return "<p>Error extracting agent metadata: " + e.getMessage() + "</p>";
		}
	}

	private static String extractValueFromString(String text, String regex) {
		try {
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
			java.util.regex.Matcher matcher = pattern.matcher(text);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		catch (Exception e) {
			// Ignore parsing errors
		}
		return null;
	}

	private static String extractCostFromString(String text) {
		try {
			// Extract input and output token costs and calculate total
			String inputCostStr = extractValueFromString(text, "inputTokenCost=([0-9.E-]+)");
			String outputCostStr = extractValueFromString(text, "outputTokenCost=([0-9.E-]+)");

			if (inputCostStr != null && outputCostStr != null) {
				double inputCost = Double.parseDouble(inputCostStr);
				double outputCost = Double.parseDouble(outputCostStr);
				double totalCost = inputCost + outputCost;
				return String.format("%.4f", totalCost);
			}
		}
		catch (Exception e) {
			// Ignore parsing errors
		}
		return null;
	}

	private static String generateLogTailHtml(Path runRoot) {
		try {
			Path logFile = runRoot.resolve("run.log");
			if (!Files.exists(logFile)) {
				return "<p>No log file available.</p>";
			}

			List<String> lines = Files.readAllLines(logFile);
			// Show last 200 lines or all if fewer
			int start = Math.max(0, lines.size() - 200);
			String logContent = String.join("\n", lines.subList(start, lines.size()));

			return "<div class=\"log-container\">"
					+ logContent.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</div>";
		}
		catch (Exception e) {
			return "<p>Error reading log file: " + e.getMessage() + "</p>";
		}
	}

}
