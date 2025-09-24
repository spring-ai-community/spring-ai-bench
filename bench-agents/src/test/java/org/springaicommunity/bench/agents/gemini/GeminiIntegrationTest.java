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
package org.springaicommunity.bench.agents.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;
import org.springaicommunity.agents.geminisdk.GeminiClient;
import org.springaicommunity.agents.geminisdk.transport.CLIOptions;
import org.springaicommunity.bench.agents.runner.GeminiAgentRunner;
import org.springaicommunity.bench.agents.verifier.HelloWorldVerifier;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Integration test for Gemini agent runner. Requires GEMINI_API_KEY environment variable
 * and Gemini CLI.
 */
@EnabledIf("hasGeminiApiKey")
@Timeout(120) // Cap runtime at 2 minutes
class GeminiIntegrationTest {

	private Path tempWorkspace;

	private GeminiAgentRunner agentRunner;

	/** Check if either GEMINI_API_KEY or GOOGLE_API_KEY is available. */
	static boolean hasGeminiApiKey() {
		String geminiKey = System.getenv("GEMINI_API_KEY");
		String googleKey = System.getenv("GOOGLE_API_KEY");
		return (geminiKey != null && !geminiKey.trim().isEmpty()) || (googleKey != null && !googleKey.trim().isEmpty());
	}

	@BeforeEach
	void setUp() throws Exception {
		tempWorkspace = Files.createTempDirectory("gemini-test-");

		// Create GeminiAgentModel with default options
		GeminiAgentOptions options = new GeminiAgentOptions();
		options.setYolo(true);
		options.setTimeout(Duration.ofMinutes(2));
		options.setModel("gemini-2.0-flash-exp"); // Latest Gemini model

		// Create Gemini CLI with debug options and yolo mode
		CLIOptions cliOptions = CLIOptions.builder().debug(true).yoloMode(true).build();
		GeminiClient client = GeminiClient.create(cliOptions, tempWorkspace);
		GeminiAgentModel agentModel = new GeminiAgentModel(client, options,
				new org.springaicommunity.agents.model.sandbox.LocalSandbox(tempWorkspace));

		assumeTrue(agentModel.isAvailable(), "GeminiAgentModel not available");

		agentRunner = new GeminiAgentRunner(agentModel, new HelloWorldVerifier());
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
	void helloWorld_case_passes() throws Exception {
		System.out.println("Testing Gemini agent with hello world task");
		System.out.println("Workspace: " + tempWorkspace);

		// Create AgentSpec with correct parameter order: kind, model, autoApprove,
		// prompt, genParams,
		// role
		AgentSpec spec = new AgentSpec("hello-world", // kind
				"gemini-2.0-flash-exp", // model (Latest Gemini)
				null, // autoApprove
				"Create a file named hello.txt with EXACT contents: Hello World!", // prompt
				null, // genParams
				null // role
		);

		System.out.println("Running Gemini agent...");

		// Run the agent through the full pipeline
		AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

		System.out.println("Gemini agent completed with exit code: " + result.exitCode());
		System.out.println("Log file: " + result.logFile());
		System.out.println("Duration: " + result.durationMillis() + "ms");

		// Verify the result
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.logFile()).exists();

		// Verify the hello.txt file was created with correct content
		Path helloFile = tempWorkspace.resolve("hello.txt");
		assertThat(helloFile).exists();

		String content = Files.readString(helloFile);
		assertThat(content).isEqualTo("Hello World!");

		System.out.println("✅ SUCCESS: Gemini agent created hello.txt with correct content!");
		System.out.println("File verified: " + content);
	}

}
