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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springaicommunity.bench.agents.hello.HelloWorldAgentRunner;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Integration test for the bench-agents infrastructure using HelloWorldAgentRunner. This
 * test verifies the complete pipeline including verification and reporting.
 */
class BenchAgentsIntegrationTest {

	private Path tempWorkspace;

	private HelloWorldAgentRunner agentRunner;

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("bench-agents-test-");
		agentRunner = new HelloWorldAgentRunner();
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
	void benchAgents_integration_pipeline_works() throws Exception {
		// Create AgentSpec for hello world task
		AgentSpec spec = new AgentSpec("hello-world", "Create a file named hello.txt with contents: Hello World!", null, // model
																															// -
																															// will
																															// use
																															// default
				null, // genParams
				null, // autoApprove
				null // role
		);

		// Run the agent through the full pipeline
		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(1));

		// Verify the result
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.logFile()).exists();
		assertThat(result.durationMillis()).isGreaterThanOrEqualTo(0);

		// Verify the hello.txt file was created with correct content
		Path helloFile = tempWorkspace.resolve("hello.txt");
		assertThat(helloFile).exists();

		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");

		System.out.println("✅ SUCCESS: Bench-agents integration pipeline works end-to-end");
	}

	@Test
	void benchAgents_report_generation_works() throws Exception {
		// Create AgentSpec
		AgentSpec spec = new AgentSpec("hello-world", "Create hello.txt", null, null, null, null);

		// Run the agent
		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(1));

		// Verify reports were generated
		Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
		assertThat(reportsDir).exists();

		// Find the most recent run directory (sorted by creation time)
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

		// Verify JSON report contains expected fields
		String jsonContent = Files.readString(jsonReport);
		assertThat(jsonContent).contains("\"success\" : true");
		assertThat(jsonContent).contains("\"caseId\" : \"hello-world\"");
		assertThat(jsonContent).contains("\"provenance\"");
		assertThat(jsonContent).contains("\"benchVersion\"");

		// Verify HTML report contains expected structure
		String htmlContent = Files.readString(htmlReport);
		assertThat(htmlContent).contains("Agent Execution Report");
		assertThat(htmlContent).contains("SUCCESS");
		assertThat(htmlContent).contains("Verification Checks");

		System.out.println("✅ SUCCESS: Report generation works with proper structure");
	}

	@Test
	void benchAgents_verification_system_works() throws Exception {
		// Test with a scenario that should pass verification
		AgentSpec spec = new AgentSpec("hello-world", "Create hello.txt with exact content: Hello World!", null, null,
				null, null);

		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(1));

		// Should succeed because HelloWorldAgentModel creates the correct file
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
		assertThat(jsonContent).contains("\"pass\" : true"); // Verification checks passed

		System.out.println("✅ SUCCESS: Verification system correctly validates agent output");
	}

}
