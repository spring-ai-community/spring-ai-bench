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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.core.spec.AgentSpec;

/**
 * Integration test for HelloWorldAgentRunner that validates the full integration between
 * Spring AI Bench and Spring AI Agents APIs.
 */
class HelloWorldIntegrationTest {

	@TempDir
	Path tempDir;

	@Test
	void testHelloWorldAgentCreatesFile() throws Exception {
		// Create agent runner
		var runner = new HelloWorldAgentRunner();

		// Create spec
		var spec = new AgentSpec("hello", // kind
				"hello-v1", // model
				true, // autoApprove
				"Create hello.txt with 'Hello World!'", // prompt
				null, // genParams
				"test" // role
		);

		// Run agent
		var result = runner.run(tempDir, spec, Duration.ofSeconds(10));

		// Verify results
		assertThat(result.succeeded()).isTrue();
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.durationMillis()).isGreaterThan(0);

		// Verify file was created
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();
		assertThat(Files.readString(helloFile)).isEqualTo("Hello World!");
	}

	@Test
	void testHelloWorldAgentWithCustomParams() throws Exception {
		// Create agent runner
		var runner = new HelloWorldAgentRunner();

		// Create spec with custom parameters
		Map<String, Object> genParams = Map.of("custom_param", "value", "test_mode", true);

		var spec = new AgentSpec("hello", // kind
				"hello-v2", // model
				false, // autoApprove
				"Create hello.txt file", // prompt
				genParams, // genParams
				"developer" // role
		);

		// Run agent
		var result = runner.run(tempDir, spec, Duration.ofSeconds(5));

		// Verify results - should still succeed regardless of params
		assertThat(result.succeeded()).isTrue();
		assertThat(result.exitCode()).isEqualTo(0);

		// Verify file was created
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();
		assertThat(Files.readString(helloFile)).isEqualTo("Hello World!");
	}

	@Test
	void testHelloWorldAgentWithShortTimeout() throws Exception {
		// Create agent runner
		var runner = new HelloWorldAgentRunner();

		// Create spec
		var spec = new AgentSpec("hello", // kind
				"hello-v1", // model
				true, // autoApprove
				"Create hello.txt with 'Hello World!'", // prompt
				null, // genParams
				"test" // role
		);

		// Run agent with very short timeout (but still should succeed since it's fast)
		var result = runner.run(tempDir, spec, Duration.ofMillis(100));

		// Should still succeed as HelloWorldAgentModel is very fast
		assertThat(result.succeeded()).isTrue();
		assertThat(result.exitCode()).isEqualTo(0);

		// Verify file was created
		Path helloFile = tempDir.resolve("hello.txt");
		assertThat(Files.exists(helloFile)).isTrue();
		assertThat(Files.readString(helloFile)).isEqualTo("Hello World!");
	}

}
