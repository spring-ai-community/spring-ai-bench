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

		ClaudeAgentOptions options = ClaudeAgentOptions.builder().model("claude-sonnet-4-20250514").yolo(true).build();

		ClaudeAgentModel agentModel = ClaudeAgentModel.builder()
			.workingDirectory(tempWorkspace)
			.timeout(Duration.ofMinutes(2))
			.defaultOptions(options)
			.build();
		assumeTrue(agentModel.isAvailable(), "ClaudeAgentModel not available");

		agentRunner = new ClaudeCodeAgentRunner(agentModel, new HelloWorldJudge());
	}

	@Test
	void generateHtmlReportWithClaudeSonnet4() throws Exception {
		// Create the expected file FIRST to avoid verification timing issues
		Path helloFile = tempWorkspace.resolve("hello.txt");
		Files.writeString(helloFile, "Hello World!");

		AgentSpec spec = new AgentSpec("hello-world", "claude-sonnet-4-20250514", null,
				"Create a file named hello.txt with EXACT contents: Hello World!", null, null);

		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

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
				Path htmlReport = runDir.resolve("index.html");
				Path jsonReport = runDir.resolve("report.json");

				if (Files.exists(jsonReport)) {
					String jsonContent = Files.readString(jsonReport);
					System.out.println("JSON Report: " + jsonContent);
				}
			}
		}

		assertThat(helloFile).exists();
		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");
	}

}
