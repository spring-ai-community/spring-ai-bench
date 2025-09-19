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
package org.springaicommunity.bench.agents.debug;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;

/** Debug test to understand ClaudeCodeAgentModel behavior. */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeCodeDebugTest {

	private Path tempWorkspace;

	private ClaudeCodeAgentModel agentModel;

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("claude-debug-test-");

		// Create ClaudeCodeAgentModel with debug configuration
		ClaudeCodeAgentOptions options = new ClaudeCodeAgentOptions();
		options.setYolo(true); // Skip permissions for testing
		options.setTimeout(Duration.ofMinutes(2));

		// Create client with the specific working directory
		ClaudeCodeClient client = ClaudeCodeClient
			.create(org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions.builder()
				.timeout(Duration.ofMinutes(2))
				.permissionMode(org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.BYPASS_PERMISSIONS)
				.build(), tempWorkspace);

		agentModel = new ClaudeCodeAgentModel(client, options);
		assumeTrue(agentModel.isAvailable(), "ClaudeCodeAgentModel not available");
	}

	@Test
	void debugClaudeCodeExecution() throws Exception {
		System.out.println("Workspace: " + tempWorkspace);

		// Debug: Print environment variables
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		System.out.println("ANTHROPIC_API_KEY in test process: "
				+ (apiKey != null ? "present (length=" + apiKey.length() + ")" : "null"));
		System.out.println("Environment variables containing 'ANTHROPIC':");
		System.getenv()
			.entrySet()
			.stream()
			.filter(e -> e.getKey().contains("ANTHROPIC"))
			.forEach(
					e -> System.out.println("  " + e.getKey() + " = " + (e.getValue() != null ? "[present]" : "null")));

		// Create simple AgentOptions for testing
		AgentOptions simpleOptions = new AgentOptions() {
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
				return "claude-sonnet-4-0";
			}

			@Override
			public Map<String, Object> getExtras() {
				return Map.of("yolo", true);
			}
		};

		// Create simple task request
		AgentTaskRequest request = new AgentTaskRequest(
				"Create a file named hello.txt with EXACT contents: Hello World!", tempWorkspace, simpleOptions);

		System.out.println("Calling agent model...");

		// Call the agent
		AgentResponse response = agentModel.call(request);

		System.out.println("Response: " + response);
		if (!response.getResults().isEmpty()) {
			System.out.println("First result: " + response.getResults().get(0).getOutput());
		}

		// Check if file was created
		Path helloFile = tempWorkspace.resolve("hello.txt");
		System.out.println("Hello file exists: " + Files.exists(helloFile));

		if (Files.exists(helloFile)) {
			String content = Files.readString(helloFile);
			System.out.println("Hello file content: '" + content + "'");
		}

		// List all files in workspace
		System.out.println("Files in workspace:");
		Files.list(tempWorkspace).forEach(System.out::println);
	}

}
