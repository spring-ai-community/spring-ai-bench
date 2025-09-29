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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Integration test for HelloWorldAIAgentRunner that validates the full integration
 * between Spring AI Bench and Spring AI Agents via JBang, testing real AI agents
 * (Claude/Gemini).
 */
class HelloWorldAIIntegrationTest {

	@TempDir
	Path tempDir;

	@Test
	void testHelloWorldAIAgentWithClaude() throws Exception {
		// Check if required dependencies are available
		assumeTrue(isJBangAvailable(), "JBang is not available");
		assumeTrue(isSpringAIAgentsBuilt(), "Spring AI Agents not built locally");
		assumeTrue(hasClaudeApiKey(), "ANTHROPIC_API_KEY not set");

		// Create agent runner with Claude provider
		var runner = new HelloWorldAIAgentRunner("claude");

		// Create spec
		var spec = new AgentSpec("hello-ai", // kind
				"claude-sonnet-4-20250514", // model
				true, // autoApprove
				"Create hello.txt with 'Hello World!' using AI", // prompt
				null, // genParams
				"ai-tester" // role
		);

		// Run agent with extended timeout for AI processing
		var result = runner.run(tempDir, spec, Duration.ofMinutes(3));

		// Verify results
		assertThat(result.succeeded()).isTrue();
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.durationMillis()).isGreaterThan(0);

		// Verify file was created by AI agent
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();

		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");
	}

	@Test
	void testHelloWorldAIAgentWithGemini() throws Exception {
		// Check if required dependencies are available
		assumeTrue(isJBangAvailable(), "JBang is not available");
		assumeTrue(isSpringAIAgentsBuilt(), "Spring AI Agents not built locally");
		assumeTrue(hasGeminiApiKey(), "GEMINI_API_KEY not set");

		// Create agent runner with Gemini provider
		var runner = new HelloWorldAIAgentRunner("gemini");

		// Create spec
		var spec = new AgentSpec("hello-ai", // kind
				"gemini-2.0-flash-exp", // model
				true, // autoApprove
				"Create hello.txt with 'Hello World!' using AI", // prompt
				null, // genParams
				"ai-tester" // role
		);

		// Run agent with extended timeout for AI processing
		var result = runner.run(tempDir, spec, Duration.ofMinutes(3));

		// Verify results
		assertThat(result.succeeded()).isTrue();
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.durationMillis()).isGreaterThan(0);

		// Verify file was created by AI agent
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();

		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");
	}

	@Test
	void testHelloWorldAIAgentAvailability() {
		var model = new HelloWorldAIAgentModel();

		if (isJBangAvailable() && isSpringAIAgentsBuilt()) {
			assertThat(model.isAvailable()).isTrue();
		}
		else {
			assertThat(model.isAvailable()).isFalse();
		}
	}

	@Test
	void testHelloWorldAIAgentWithCustomParams() throws Exception {
		// Check if required dependencies are available
		assumeTrue(isJBangAvailable(), "JBang is not available");
		assumeTrue(isSpringAIAgentsBuilt(), "Spring AI Agents not built locally");
		assumeTrue(hasClaudeApiKey(), "ANTHROPIC_API_KEY not set");

		// Create agent runner with default provider (Claude)
		var runner = new HelloWorldAIAgentRunner();

		// Create spec with custom parameters
		Map<String, Object> genParams = Map.of("ai_mode", "creative", "test_mode", true);

		var spec = new AgentSpec("hello-ai", // kind
				"claude-sonnet-4-20250514", // model
				false, // autoApprove
				"Create hello.txt file using AI creativity", // prompt
				genParams, // genParams
				"developer" // role
		);

		// Run agent
		var result = runner.run(tempDir, spec, Duration.ofMinutes(2));

		// Verify results - should still succeed regardless of params
		assertThat(result.succeeded()).isTrue();
		assertThat(result.exitCode()).isEqualTo(0);

		// Verify file was created
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();
		assertThat(Files.readString(helloFile)).isEqualTo("Hello World!");
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