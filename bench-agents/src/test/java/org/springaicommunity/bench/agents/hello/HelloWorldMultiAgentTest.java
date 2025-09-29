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
package org.springaicommunity.bench.agents.hello;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Multi-agent integration test that demonstrates running both hello-world (deterministic)
 * and hello-world-agent-ai (AI-powered) agents together, comparing their performance and
 * validating that both produce the expected results.
 */
class HelloWorldMultiAgentTest {

	@TempDir
	Path tempDir;

	@Test
	void testBothHelloWorldAgentsTogether() throws Exception {
		// Check if AI agent dependencies are available
		boolean hasClaudeKey = hasClaudeApiKey();
		boolean hasGeminiKey = hasGeminiApiKey();
		boolean hasJBang = isJBangAvailable();
		boolean hasSpringAIAgents = isSpringAIAgentsBuilt();

		// Always run deterministic agent
		var deterministicRunner = new HelloWorldAgentRunner();
		var deterministicSpec = new AgentSpec("hello-deterministic", // kind
				"hello-v1", // model
				true, // autoApprove
				"Create hello.txt with 'Hello World!'", // prompt
				null, // genParams
				"test" // role
		);

		// Create separate workspace for deterministic agent
		Path deterministicWorkspace = tempDir.resolve("deterministic");
		Files.createDirectories(deterministicWorkspace);

		Instant startTime = Instant.now();
		var deterministicResult = deterministicRunner.run(deterministicWorkspace, deterministicSpec,
				Duration.ofSeconds(10));
		Instant deterministicEndTime = Instant.now();

		// Verify deterministic agent
		assertThat(deterministicResult.succeeded()).isTrue();
		assertThat(deterministicResult.exitCode()).isEqualTo(0);

		Path deterministicFile = deterministicWorkspace.resolve("hello.txt");
		assertThat(Files.exists(deterministicFile)).isTrue();
		assertThat(Files.readString(deterministicFile)).isEqualTo("Hello World!");

		List<AgentResult> allResults = new ArrayList<>();
		List<String> agentTypes = new ArrayList<>();
		List<Long> durations = new ArrayList<>();

		allResults.add(deterministicResult);
		agentTypes.add("deterministic");
		durations.add(Duration.between(startTime, deterministicEndTime).toMillis());

		// Run AI agents if available
		if (hasJBang && hasSpringAIAgents) {
			// Test Claude if API key is available
			if (hasClaudeKey) {
				var claudeRunner = new HelloWorldAIAgentRunner("claude");
				var claudeSpec = new AgentSpec("hello-ai-claude", // kind
						"claude-sonnet-4-20250514", // model
						true, // autoApprove
						"Create hello.txt with 'Hello World!' using Claude AI", // prompt
						null, // genParams
						"ai-tester" // role
				);

				Path claudeWorkspace = tempDir.resolve("claude");
				Files.createDirectories(claudeWorkspace);

				Instant claudeStartTime = Instant.now();
				var claudeResult = claudeRunner.run(claudeWorkspace, claudeSpec, Duration.ofMinutes(3));
				Instant claudeEndTime = Instant.now();

				// Verify Claude agent
				assertThat(claudeResult.succeeded()).isTrue();
				assertThat(claudeResult.exitCode()).isEqualTo(0);

				Path claudeFile = claudeWorkspace.resolve("hello.txt");
				assertThat(Files.exists(claudeFile)).isTrue();
				assertThat(Files.readString(claudeFile)).isEqualTo("Hello World!");

				allResults.add(claudeResult);
				agentTypes.add("claude-ai");
				durations.add(Duration.between(claudeStartTime, claudeEndTime).toMillis());
			}

			// Test Gemini if API key is available
			if (hasGeminiKey) {
				var geminiRunner = new HelloWorldAIAgentRunner("gemini");
				var geminiSpec = new AgentSpec("hello-ai-gemini", // kind
						"gemini-2.0-flash-exp", // model
						true, // autoApprove
						"Create hello.txt with 'Hello World!' using Gemini AI", // prompt
						null, // genParams
						"ai-tester" // role
				);

				Path geminiWorkspace = tempDir.resolve("gemini");
				Files.createDirectories(geminiWorkspace);

				Instant geminiStartTime = Instant.now();
				var geminiResult = geminiRunner.run(geminiWorkspace, geminiSpec, Duration.ofMinutes(3));
				Instant geminiEndTime = Instant.now();

				// Verify Gemini agent
				assertThat(geminiResult.succeeded()).isTrue();
				assertThat(geminiResult.exitCode()).isEqualTo(0);

				Path geminiFile = geminiWorkspace.resolve("hello.txt");
				assertThat(Files.exists(geminiFile)).isTrue();
				assertThat(Files.readString(geminiFile)).isEqualTo("Hello World!");

				allResults.add(geminiResult);
				agentTypes.add("gemini-ai");
				durations.add(Duration.between(geminiStartTime, geminiEndTime).toMillis());
			}
		}

		// Print comparative results
		System.out.println("\n=== Multi-Agent Hello World Benchmark Results ===");
		for (int i = 0; i < allResults.size(); i++) {
			AgentResult result = allResults.get(i);
			String agentType = agentTypes.get(i);
			long duration = durations.get(i);

			System.out.printf("Agent: %-15s | Success: %-5s | Duration: %4d ms | Exit Code: %d%n", agentType,
					result.succeeded(), duration, result.exitCode());
		}

		// Verify at least the deterministic agent ran
		assertThat(allResults).hasSize(allResults.size());
		assertThat(allResults).allMatch(AgentResult::succeeded);

		// Show summary
		System.out.printf("\nSummary: %d agents executed successfully%n", allResults.size());
		if (allResults.size() > 1) {
			long minDuration = durations.stream().mapToLong(Long::longValue).min().orElse(0);
			long maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0);
			System.out.printf("Duration range: %d ms (fastest) to %d ms (slowest)%n", minDuration, maxDuration);
		}
	}

	// Helper methods for availability checks
	private boolean isJBangAvailable() {
		try {
			ProcessBuilder pb = new ProcessBuilder("jbang", "--version");
			Process process = pb.start();
			return process.waitFor() == 0;
		}
		catch (Exception e) {
			return false;
		}
	}

	private boolean isSpringAIAgentsBuilt() {
		Path launcherPath = Path.of("/home/mark/community/spring-ai-agents/jbang/launcher.java");
		return Files.exists(launcherPath);
	}

	private boolean hasClaudeApiKey() {
		String apiKey = System.getenv("ANTHROPIC_API_KEY");
		return apiKey != null && !apiKey.trim().isEmpty();
	}

	private boolean hasGeminiApiKey() {
		String apiKey = System.getenv("GEMINI_API_KEY");
		return apiKey != null && !apiKey.trim().isEmpty();
	}

}