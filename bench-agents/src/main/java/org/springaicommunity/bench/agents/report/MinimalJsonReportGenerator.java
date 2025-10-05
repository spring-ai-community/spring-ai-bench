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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Generates minimal JSON reports for agent execution results. Uses the judge framework.
 */
public class MinimalJsonReportGenerator {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static Path generate(UUID runId, String caseId, boolean success, Instant startedAt, Instant finishedAt,
			long durationMs, Judgment judgment, Path runRoot, Path workspace, String provider) throws Exception {

		// Create structured report data
		Map<String, Object> report = new LinkedHashMap<>();

		// Basic information (minimal schema)
		report.put("runId", runId.toString());
		report.put("caseId", caseId);
		report.put("success", success);
		report.put("reason", judgment != null ? judgment.reasoning() : "No judgment");
		report.put("startedAt", startedAt.toString());
		report.put("finishedAt", finishedAt.toString());
		report.put("durationMs", durationMs);

		// Relative paths
		report.put("logPath", "run.log");
		report.put("workspacePath", "../" + runRoot.getParent().relativize(runRoot.getParent()).toString());

		// Judgment checks
		if (judgment != null && !judgment.checks().isEmpty()) {
			var checks = judgment.checks()
				.stream()
				.map(check -> Map.of("name", check.name(), "passed", check.passed(), "message", check.message()))
				.toList();
			report.put("checks", checks);
		}

		// Enhanced provenance with agent information
		Map<String, Object> provenance = new LinkedHashMap<>();
		provenance.put("benchVersion", "0.1.0-SNAPSHOT");
		provenance.put("agentsVersion", "0.1.0-SNAPSHOT");
		provenance.put("generator", "Spring AI Bench");
		provenance.put("reportFormat", "1.0");
		provenance.put("generatedAt", Instant.now().toString());

		// Agent info as nested object
		Map<String, Object> agentInfo = new LinkedHashMap<>();
		agentInfo.put("provider", provider);
		agentInfo.put("workspacePath", workspace.toAbsolutePath().toString());
		provenance.put("agent", agentInfo);

		report.put("provenance", provenance);

		// Write JSON report
		Path jsonFile = runRoot.resolve("report.json");
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), report);

		return jsonFile;
	}

	// Backward compatibility overload
	public static Path generate(UUID runId, String caseId, boolean success, Instant startedAt, Instant finishedAt,
			long durationMs, Judgment judgment, Path runRoot) throws Exception {

		// Use default values for new parameters
		Path workspace = runRoot.getParent().resolve("workspace-unknown");
		String provider = "unknown";

		return generate(runId, caseId, success, startedAt, finishedAt, durationMs, judgment, runRoot, workspace,
				provider);
	}

}
