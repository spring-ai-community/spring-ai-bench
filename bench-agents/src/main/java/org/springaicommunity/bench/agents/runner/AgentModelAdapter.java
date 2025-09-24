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
package org.springaicommunity.bench.agents.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.model.*;
import org.springaicommunity.bench.agents.support.SimpleLogCapture;
import org.springaicommunity.bench.agents.verifier.*;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.run.AgentRunner;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Adapter that bridges between Spring AI Bench and Spring AI Agents APIs. Uses
 * verification system to determine success and captures detailed logs.
 */
public class AgentModelAdapter implements AgentRunner {

	private final AgentModel agentModel;

	private final SuccessVerifier verifier;

	private final WorkspaceService workspaceService;

	private final ReportService reportService;

	public AgentModelAdapter(AgentModel agentModel, SuccessVerifier verifier, WorkspaceService workspaceService,
			ReportService reportService) {
		this.agentModel = agentModel;
		this.verifier = verifier;
		this.workspaceService = workspaceService;
		this.reportService = reportService;
	}

	// Constructor for backward compatibility with verifier
	public AgentModelAdapter(AgentModel agentModel, SuccessVerifier verifier) {
		this.agentModel = agentModel;
		this.verifier = verifier;
		this.workspaceService = new WorkspaceService();
		this.reportService = new ReportService();
	}

	// Constructor for backward compatibility without verifier
	public AgentModelAdapter(AgentModel agentModel) {
		this.agentModel = agentModel;
		this.verifier = null; // Will use old fallback logic
		this.workspaceService = new WorkspaceService();
		this.reportService = new ReportService();
	}

	@Override
	public AgentResult run(Path workspace, AgentSpec spec, Duration timeout) throws Exception {
		Instant startedAt = Instant.now();

		// Create run directory structure for reports
		UUID runId = UUID.randomUUID();
		Path runRoot = workspace.getParent().resolve("bench-reports").resolve(runId.toString());
		Path actualWorkspace = workspace;

		SimpleLogCapture logger = null;

		try {
			// Set up logging
			logger = new SimpleLogCapture(runRoot, runId);
			logger.log("ADAPTER", "Starting " + agentModel.getClass().getSimpleName());
			logger.log("SETUP", "Workspace: " + actualWorkspace);
			logger.log("SETUP", "Run root: " + runRoot);
			logger.log("SETUP", "Run ID: " + runId);

			// Clean and create workspace using service
			workspaceService.cleanWorkspace(actualWorkspace);
			logger.log("WORKSPACE", "Workspace cleaned successfully");

			// Configure agent model for workspace
			AgentModel effectiveAgentModel = configureAgentForWorkspace(agentModel, actualWorkspace, timeout, logger);

			// Create AgentOptions and TaskRequest
			AgentOptions options = createAgentOptions(spec, timeout, actualWorkspace);
			Path absoluteWorkspace = actualWorkspace.toAbsolutePath();

			var request = new AgentTaskRequest(spec.prompt(), absoluteWorkspace, options);

			logger.log("AGENT", "Executing agent task");
			AgentResponse response = effectiveAgentModel.call(request);

			// Log agent response summary
			logger.log("AGENT", "Agent call completed. Results: " + response.getResults().size());
			if (!response.getResults().isEmpty()) {
				var firstGen = response.getResults().get(0);
				logger.log("AGENT", "First generation output: "
						+ firstGen.getOutput().substring(0, Math.min(100, firstGen.getOutput().length())));
			}

			// Verify results
			boolean success;
			VerificationResult verificationResult = null;

			if (verifier != null) {
				logger.log("VERIFIER", "Starting verification");
				VerificationContext context = new VerificationContext(absoluteWorkspace.getParent(),
						absoluteWorkspace.getFileName(), startedAt);
				verificationResult = verifier.verify(context);
				success = verificationResult.success();

				String checkSummary = verificationResult.checks()
					.stream()
					.map(check -> check.name() + ":" + (check.pass() ? "PASS" : "FAIL"))
					.reduce((a, b) -> a + " " + b)
					.orElse("no checks");
				logger.log("VERIFIER", checkSummary);
			}
			else {
				success = isSuccessfulResponse(response);
				logger.log("VERIFIER", "Using fallback heuristic: " + (success ? "PASS" : "FAIL"));
			}

			Instant finishedAt = Instant.now();
			long duration = Duration.between(startedAt, finishedAt).toMillis();
			if (duration == 0) {
				duration = 1;
			}

			logger.log("RESULT", (success ? "SUCCESS" : "FAILURE") + ": "
					+ (verificationResult != null ? verificationResult.reason() : "heuristic check"));
			logger.log("FINAL", "Exit code: " + (success ? 0 : 1) + ", Duration: " + duration + "ms");

			if (logger != null) {
				logger.writeFooter();
			}

			// Generate reports using service
			reportService.generateReports(runId, spec.kind(), success, startedAt, finishedAt, duration,
					verificationResult, runRoot, actualWorkspace, response, effectiveAgentModel);

			return new AgentResult(success ? 0 : 1, runRoot.resolve("run.log"), duration);

		}
		catch (Exception e) {
			Instant finishedAt = Instant.now();
			long duration = Duration.between(startedAt, finishedAt).toMillis();

			if (logger != null) {
				logger.log("ERROR", "Agent execution failed: " + e.getMessage());
				logger.writeFooter();
			}

			return new AgentResult(1, runRoot.resolve("run.log"), duration);
		}
	}

	/** Configures the agent model for workspace-specific execution. */
	private AgentModel configureAgentForWorkspace(AgentModel model, Path workspace, Duration timeout,
			SimpleLogCapture logger) {
		if (model instanceof ClaudeCodeAgentModel) {
			logger.log("AGENT", "Creating workspace-specific ClaudeCodeAgentModel");
			try {
				return ClaudeCodeAgentModel.createWithWorkspaceSetup(workspace, timeout);
			}
			catch (RuntimeException e) {
				logger.log("AGENT", "Failed to create workspace-specific model, using original: " + e.getMessage());
				return model; // Fall back to original model
			}
		}
		return model; // For other agent types, return as-is
	}

	private AgentOptions createAgentOptions(AgentSpec spec, Duration timeout, Path workspace) {
		// For Gemini agents, create GeminiAgentOptions with yolo mode enabled
		if (agentModel instanceof GeminiAgentModel) {
			GeminiAgentOptions geminiOptions = new GeminiAgentOptions();
			geminiOptions.setTimeout(timeout);
			geminiOptions.setModel(spec.model() != null ? spec.model() : "gemini-2.0-flash-exp");
			geminiOptions.setYolo(true);

			// Set extras from spec
			Map<String, Object> extras = new HashMap<>();
			if (spec.genParams() != null) {
				extras.putAll(spec.genParams());
			}
			if (spec.autoApprove() != null) {
				extras.put("autoApprove", spec.autoApprove());
			}
			if (spec.role() != null) {
				extras.put("role", spec.role());
			}
			extras.put("yolo", true);
			geminiOptions.setExtras(extras);

			return geminiOptions;
		}

		// For other agents, use generic AgentOptions
		return new AgentOptions() {
			@Override
			public String getWorkingDirectory() {
				return workspace.toAbsolutePath().toString();
			}

			@Override
			public Duration getTimeout() {
				return timeout;
			}

			@Override
			public Map<String, String> getEnvironmentVariables() {
				return Map.of();
			}

			@Override
			public String getModel() {
				return spec.model() != null ? spec.model() : "default";
			}

			@Override
			public Map<String, Object> getExtras() {
				Map<String, Object> extras = new HashMap<>();
				if (spec.genParams() != null) {
					extras.putAll(spec.genParams());
				}
				if (spec.autoApprove() != null) {
					extras.put("autoApprove", spec.autoApprove());
				}
				if (spec.role() != null) {
					extras.put("role", spec.role());
				}
				extras.put("yolo", true);
				return extras;
			}
		};
	}

	/** Fallback success determination for backward compatibility. */
	private boolean isSuccessfulResponse(AgentResponse response) {
		if (response == null || response.getResults().isEmpty()) {
			return false;
		}

		return response.getResults().stream().anyMatch(generation -> {
			String content = generation.getOutput().toLowerCase();
			// Check metadata for explicit success flag
			Object success = generation.getMetadata().getProviderFields().get("success");
			if (success instanceof Boolean) {
				return (Boolean) success;
			}
			// Fall back to content analysis
			return !content.contains("failed") && !content.contains("error");
		});
	}

}
