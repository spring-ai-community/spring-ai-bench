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
package org.springaicommunity.bench.agents.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.bench.agents.runner.ClaudeCodeAgentRunner;
import org.springaicommunity.bench.agents.judge.HelloWorldJudge;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Integration test for Claude Code agent report generation with real agent provenance.
 * Tests the complete pipeline with actual Claude CLI execution and metadata capture.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeCodeReportGenerationTest {

	private Path tempWorkspace;

	private ClaudeCodeAgentRunner agentRunner;

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("claude-report-test-");

		// Create ClaudeAgentModel with real configuration
		ClaudeAgentOptions options = new ClaudeAgentOptions();
		options.setYolo(true); // Skip permissions for testing
		options.setTimeout(Duration.ofMinutes(2));

		// Create client
		ClaudeAgentClient client = ClaudeAgentClient
			.create(org.springaicommunity.agents.claude.sdk.transport.CLIOptions.builder()
				.timeout(Duration.ofMinutes(2))
				.permissionMode(org.springaicommunity.agents.claude.sdk.config.PermissionMode.BYPASS_PERMISSIONS)
				.build(), tempWorkspace);

		ClaudeAgentModel agentModel = new ClaudeAgentModel(client, options,
				new org.springaicommunity.agents.model.sandbox.LocalSandbox(tempWorkspace));
		assumeTrue(agentModel.isAvailable(), "ClaudeAgentModel not available");

		// Create agent runner with HelloWorldVerifier
		agentRunner = new ClaudeCodeAgentRunner(agentModel, new HelloWorldJudge());
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tempWorkspace != null && Files.exists(tempWorkspace)) {
			Files.walk(tempWorkspace)
				.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					}
					catch (Exception e) {
						// Best effort cleanup
					}
				});
		}
	}

	@Test
	void claudeCode_report_generation_with_real_provenance() throws Exception {
		System.out.println("Workspace: " + tempWorkspace);

		// Create AgentSpec for hello world task
		AgentSpec spec = AgentSpec.builder()
			.kind("claude-hello-world")
			.autoApprove(true)
			.prompt("Create a file named hello.txt in the current working directory (use relative path ./hello.txt) with EXACT contents: Hello World!")
			.build();

		// Run the agent through the full pipeline
		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

		// Verify the result
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.logFile()).exists();
		assertThat(result.durationMillis()).isGreaterThanOrEqualTo(0);

		// Verify the hello.txt file was created with correct content
		Path helloFile = tempWorkspace.resolve("hello.txt");
		assertThat(helloFile).exists();

		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");

		// Verify reports were generated
		Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
		assertThat(reportsDir).exists();

		// Find the most recent run directory
		Path runDir = Files.list(reportsDir).filter(Files::isDirectory).sorted((a, b) -> {
			try {
				return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
			}
			catch (Exception e) {
				return 0;
			}
		}).findFirst().orElseThrow(() -> new AssertionError("No run directory found"));

		// Verify report files exist
		Path jsonReport = runDir.resolve("report.json");
		Path htmlReport = runDir.resolve("index.html");
		Path logFile = runDir.resolve("run.log");

		assertThat(jsonReport).exists();
		assertThat(htmlReport).exists();
		assertThat(logFile).exists();

		// Verify JSON report contains Claude-specific provenance
		String jsonContent = Files.readString(jsonReport);
		assertThat(jsonContent).contains("\"success\" : true");
		assertThat(jsonContent).contains("\"caseId\" : \"claude-hello-world\"");
		assertThat(jsonContent).contains("\"provenance\"");
		assertThat(jsonContent).contains("\"benchVersion\"");

		// Claude-specific metadata should be present
		assertThat(jsonContent).contains("claude"); // Should contain claude model info
		assertThat(jsonContent).contains("duration"); // Should contain timing info

		// Verify HTML report contains expected structure
		String htmlContent = Files.readString(htmlReport);
		assertThat(htmlContent).contains("Agent Execution Report");
		assertThat(htmlContent).contains("SUCCESS");
		assertThat(htmlContent).contains("Verification Checks");
		assertThat(htmlContent).contains("claude"); // Should show claude as the agent

		System.out.println("✅ SUCCESS: Claude Code report generation works with real agent provenance");
		System.out.println("Report directory: " + runDir);
		System.out.println("JSON report size: " + Files.size(jsonReport) + " bytes");
		System.out.println("HTML report size: " + Files.size(htmlReport) + " bytes");
	}

	@Test
	void claudeCode_verification_system_works_with_real_agent() throws Exception {
		// Test with a scenario that should pass verification
		AgentSpec spec = AgentSpec.builder()
			.kind("claude-verification-test")
			.autoApprove(true)
			.prompt("Create a file named hello.txt in the current working directory (use relative path ./hello.txt) with exact content: Hello World!")
			.build();

		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

		// Should succeed because Claude creates the correct file
		assertThat(result.exitCode()).isEqualTo(0);

		// Verify the verification checks passed
		Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
		Path runDir = Files.list(reportsDir).filter(Files::isDirectory).sorted((a, b) -> {
			try {
				return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
			}
			catch (Exception e) {
				return 0;
			}
		}).findFirst().orElseThrow();

		String jsonContent = Files.readString(runDir.resolve("report.json"));
		assertThat(jsonContent).contains("\"success\" : true");
		assertThat(jsonContent).contains("\"passed\" : true"); // Judge checks passed

		System.out.println("✅ SUCCESS: Verification system works correctly with real Claude agent");
	}

}
