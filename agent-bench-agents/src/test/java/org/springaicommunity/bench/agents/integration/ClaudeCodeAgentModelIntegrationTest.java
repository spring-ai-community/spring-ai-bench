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
import org.springaicommunity.bench.agents.judge.HelloWorldJudge;
import org.springaicommunity.bench.agents.runner.ClaudeCodeAgentRunner;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Integration test for real ClaudeAgentModel execution through bench-agents. This test
 * verifies the complete pipeline with actual Claude Code calls.
 */
@Tag("agents-live")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@Timeout(180) // Cap runtime at 3 minutes for Claude execution
class ClaudeCodeAgentModelIntegrationTest {

	private Path tempWorkspace;

	private ClaudeCodeAgentRunner agentRunner;

	@BeforeAll
	static void requireClaudeCode() {
		// Check if Claude CLI is available
		assumeTrue(isCliAvailable("claude"), "Claude CLI not available on PATH");
	}

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("claude-model-test-");

		ClaudeAgentOptions options = ClaudeAgentOptions.builder().yolo(true).build();

		ClaudeAgentModel agentModel = ClaudeAgentModel.builder()
			.workingDirectory(tempWorkspace)
			.timeout(Duration.ofMinutes(2))
			.defaultOptions(options)
			.build();
		assumeTrue(agentModel.isAvailable(), "ClaudeAgentModel not available");

		agentRunner = new ClaudeCodeAgentRunner(agentModel, new HelloWorldJudge());
	}

	@AfterEach
	void tearDown() throws Exception {
		if (tempWorkspace != null && Files.exists(tempWorkspace)) {
			Files.walk(tempWorkspace).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
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
	void claudeCodeAgentModel_integration_pipeline_works() throws Exception {
		AgentSpec spec = new AgentSpec("hello-world", null, true,
				"Create a file named hello.txt in the current working directory with EXACT contents: Hello World!",
				null, null);

		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.logFile()).exists();
		assertThat(result.durationMillis()).isGreaterThan(0);

		Path helloFile = tempWorkspace.resolve("hello.txt");
		assertThat(helloFile).exists();
		String content = Files.readString(helloFile);
		assertThat(content.trim()).isEqualTo("Hello World!");
	}

	@Test
	void claudeCodeAgentModel_report_generation_with_provenance() throws Exception {
		AgentSpec spec = new AgentSpec("hello-world", "claude-sonnet-4-0", true,
				"Create hello.txt with exact content: Hello World!", null, null);

		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

		Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
		assertThat(reportsDir).exists();

		Path runDir = Files.list(reportsDir).filter(Files::isDirectory).sorted((a, b) -> {
			try {
				return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
			}
			catch (Exception e) {
				return 0;
			}
		}).findFirst().orElseThrow(() -> new AssertionError("No run directory found"));

		assertThat(runDir.resolve("report.json")).exists();
		assertThat(runDir.resolve("index.html")).exists();
		assertThat(runDir.resolve("run.log")).exists();

		String jsonContent = Files.readString(runDir.resolve("report.json"));
		assertThat(jsonContent).contains("\"success\" : true");
		assertThat(jsonContent).contains("\"caseId\" : \"hello-world\"");
		assertThat(jsonContent).contains("\"provenance\"");
	}

	@Test
	void claudeCodeAgentModel_verification_system_works() throws Exception {
		AgentSpec spec = new AgentSpec("hello-world", null, true, "Create hello.txt with exact content: Hello World!",
				null, null);

		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));
		assertThat(result.exitCode()).isEqualTo(0);

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
		assertThat(jsonContent).contains("\"passed\" : true");
	}

	private static boolean isCliAvailable(String cmd) {
		try {
			Process p = new ProcessBuilder(cmd, "--version").redirectErrorStream(true).start();
			boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
			return finished && p.exitValue() == 0;
		}
		catch (Exception e) {
			return false;
		}
	}

}
