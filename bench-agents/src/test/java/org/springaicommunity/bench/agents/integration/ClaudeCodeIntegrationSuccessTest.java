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
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

/**
 * Integration success test demonstrating that Claude Code integration works. This test
 * focuses on core functionality rather than verification logic.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeCodeIntegrationSuccessTest {

	private Path tempWorkspace;

	private ClaudeAgentModel agentModel;

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("claude-success-test-");

		ClaudeAgentOptions options = ClaudeAgentOptions.builder().yolo(true).build();

		agentModel = ClaudeAgentModel.builder()
			.workingDirectory(tempWorkspace)
			.timeout(Duration.ofMinutes(2))
			.defaultOptions(options)
			.build();
		assumeTrue(agentModel.isAvailable(), "ClaudeAgentModel not available");
	}

	@AfterEach
	void tearDown() throws Exception {
		System.out.println("Workspace preserved for manual testing: " + tempWorkspace);
	}

	@Test
	void claudeCode_integration_works_successfully() throws Exception {
		AgentOptions options = new AgentOptions() {
			@Override
			public String getWorkingDirectory() {
				return tempWorkspace.toString();
			}

			@Override
			public Duration getTimeout() {
				return Duration.ofMinutes(2);
			}

			@Override
			public Map<String, String> getEnvironmentVariables() {
				return Map.of();
			}

			@Override
			public String getModel() {
				return "sonnet";
			}

			@Override
			public Map<String, Object> getExtras() {
				return Map.of("yolo", true);
			}
		};

		AgentTaskRequest request = new AgentTaskRequest(
				"Create a file named test-success.txt with EXACT contents: Integration Success!", tempWorkspace,
				options);

		AgentResponse response = agentModel.call(request);

		assertThat(response).isNotNull();
		assertThat(response.getResults()).isNotEmpty();

		Path testFile = tempWorkspace.resolve("test-success.txt");
		assertThat(testFile).exists();

		String content = Files.readString(testFile);
		assertThat(content).isEqualTo("Integration Success!");

		assertThat(response.getMetadata()).isNotNull();
		assertThat(response.getMetadata().getModel()).contains("claude");
	}

}
