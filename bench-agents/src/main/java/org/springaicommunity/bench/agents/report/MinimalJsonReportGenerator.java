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
import org.springaicommunity.bench.agents.verifier.VerificationResult;

/**
 * Generates minimal JSON reports for agent execution results. Uses the new verification
 * system data.
 */
public class MinimalJsonReportGenerator {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static Path generate(UUID runId, String caseId, boolean success, Instant startedAt, Instant finishedAt,
			long durationMs, VerificationResult verificationResult, Path runRoot, Path workspace, String provider)
			throws Exception {

		// Create structured report data
		Map<String, Object> report = new LinkedHashMap<>();

		// Basic information (minimal schema)
		report.put("runId", runId.toString());
		report.put("caseId", caseId);
		report.put("success", success);
		report.put("reason", verificationResult != null ? verificationResult.reason() : "No verification");
		report.put("startedAt", startedAt.toString());
		report.put("finishedAt", finishedAt.toString());
		report.put("durationMs", durationMs);

		// Relative paths
		report.put("logPath", "run.log");
		report.put("workspacePath", "../" + runRoot.getParent().relativize(runRoot.getParent()).toString());

		// Verification checks
		if (verificationResult != null && !verificationResult.checks().isEmpty()) {
			var checks = verificationResult.checks()
				.stream()
				.map(check -> Map.of("name", check.name(), "pass", check.pass(), "info", check.info()))
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
			long durationMs, VerificationResult verificationResult, Path runRoot) throws Exception {

		// Use default values for new parameters
		Path workspace = runRoot.getParent().resolve("workspace-unknown");
		String provider = "unknown";

		return generate(runId, caseId, success, startedAt, finishedAt, durationMs, verificationResult, runRoot,
				workspace, provider);
	}

}
