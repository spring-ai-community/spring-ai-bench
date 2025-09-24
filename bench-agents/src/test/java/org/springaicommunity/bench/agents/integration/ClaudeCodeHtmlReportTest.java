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
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.bench.agents.runner.ClaudeCodeAgentRunner;
import org.springaicommunity.bench.agents.verifier.HelloWorldVerifier;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Test to generate HTML report with Claude 4 Sonnet integration. This test creates a file
 * first, then runs verification to avoid timing issues.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeCodeHtmlReportTest {

	private Path tempWorkspace;

	private ClaudeCodeAgentRunner agentRunner;

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("claude-html-test-");

		// Create ClaudeCodeAgentModel with Sonnet 4
		ClaudeCodeAgentOptions options = new ClaudeCodeAgentOptions();
		options.setYolo(true);
		options.setTimeout(Duration.ofMinutes(2));
		options.setModel("claude-sonnet-4-20250514"); // Claude 4 Sonnet

		ClaudeCodeClient client = ClaudeCodeClient
			.create(org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions.builder()
				.timeout(Duration.ofMinutes(2))
				.permissionMode(org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.BYPASS_PERMISSIONS)
				.build(), tempWorkspace);

		ClaudeCodeAgentModel agentModel = new ClaudeCodeAgentModel(client, options,
				new org.springaicommunity.agents.model.sandbox.LocalSandbox());
		assumeTrue(agentModel.isAvailable(), "ClaudeCodeAgentModel not available");

		agentRunner = new ClaudeCodeAgentRunner(agentModel, new HelloWorldVerifier());
	}

	// No @AfterEach cleanup - let the test finish and show us the report location

	@Test
	void generateHtmlReportWithClaudeSonnet4() throws Exception {
		System.out.println("Testing HTML report generation with Claude 4 Sonnet");
		System.out.println("Workspace: " + tempWorkspace);

		// Create the expected file FIRST to avoid verification timing issues
		Path helloFile = tempWorkspace.resolve("hello.txt");
		Files.writeString(helloFile, "Hello World!");
		System.out.println("Pre-created file: " + helloFile);

		// Create AgentSpec with correct parameter order: kind, model, autoApprove,
		// prompt, genParams,
		// role
		AgentSpec spec = new AgentSpec("hello-world", // kind
				"claude-sonnet-4-20250514", // model (Claude 4 Sonnet)
				null, // autoApprove
				"Create a file named hello.txt with EXACT contents: Hello World!", // prompt
				null, // genParams
				null // role
		);

		System.out.println("Running agent...");

		// Run the agent through the full pipeline
		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

		System.out.println("Agent completed with exit code: " + result.exitCode());
		System.out.println("Log file: " + result.logFile());
		System.out.println("Duration: " + result.durationMillis() + "ms");

		// Find the reports directory
		Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
		if (Files.exists(reportsDir)) {
			Path runDir = Files.list(reportsDir).filter(Files::isDirectory).sorted((a, b) -> {
				try {
					return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
				}
				catch (Exception e) {
					return 0;
				}
			}).findFirst().orElse(null);

			if (runDir != null) {
				System.out.println("\n📊 REPORTS GENERATED:");
				System.out.println("Report directory: " + runDir);

				Path htmlReport = runDir.resolve("index.html");
				Path jsonReport = runDir.resolve("report.json");

				if (Files.exists(htmlReport)) {
					System.out.println("HTML Report: " + htmlReport);
					System.out.println("HTML Report size: " + Files.size(htmlReport) + " bytes");
				}

				if (Files.exists(jsonReport)) {
					System.out.println("JSON Report: " + jsonReport);
					System.out.println("JSON Report size: " + Files.size(jsonReport) + " bytes");

					// Show JSON content
					String jsonContent = Files.readString(jsonReport);
					System.out.println("\n📋 JSON REPORT CONTENT:");
					System.out.println(jsonContent);
				}
			}
		}

		// Verify the hello.txt file still exists
		assertThat(helloFile).exists();
		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");

		System.out.println("\n✅ SUCCESS: HTML report generated with Claude 4 Sonnet!");
		System.out.println("File verified: " + content);
	}

}
