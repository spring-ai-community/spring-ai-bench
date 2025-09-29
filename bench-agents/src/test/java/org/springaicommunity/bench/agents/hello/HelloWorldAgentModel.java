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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springaicommunity.agents.model.*;

/**
 * Deterministic "Hello World" agent model for testing the integration between Spring AI
 * Bench and Spring AI Agents. This agent always creates a hello.txt file with "Hello
 * World!" content, providing predictable behavior for baseline tests.
 */
public class HelloWorldAgentModel implements AgentModel {

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		Path workspace = request.workingDirectory();
		Path helloFile = workspace.resolve("hello.txt");

		try {
			// Ensure workspace directory exists
			Files.createDirectories(workspace);

			// Create hello.txt with exact content
			Files.writeString(helloFile, "Hello World!");

			// Create successful generation
			var metadata = new AgentGenerationMetadata("SUCCESS",
					Map.of("file_created", helloFile.toString(), "content", "Hello World!", "success", true));

			var generation = new AgentGeneration("Successfully created hello.txt with 'Hello World!' content",
					metadata);

			return new AgentResponse(List.of(generation));

		}
		catch (Exception e) {
			// Create failure generation
			var metadata = new AgentGenerationMetadata("ERROR", Map.of("error", e.getMessage(), "success", false));

			var generation = new AgentGeneration("Failed to create hello.txt: " + e.getMessage(), metadata);

			return new AgentResponse(List.of(generation));
		}
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

}
