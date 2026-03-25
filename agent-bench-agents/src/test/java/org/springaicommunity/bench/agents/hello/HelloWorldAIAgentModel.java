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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springaicommunity.agents.model.*;

/**
 * AI-powered "Hello World" agent model that invokes the hello-world-agent-ai via JBang.
 * This demonstrates end-to-end integration between Spring AI Bench and Spring AI Agents,
 * using real AI agents (Claude Code or Gemini) to create files.
 */
public class HelloWorldAIAgentModel implements AgentModel {

	private final String provider;

	public HelloWorldAIAgentModel() {
		this("claude"); // Default to Claude
	}

	public HelloWorldAIAgentModel(String provider) {
		this.provider = provider;
	}

	@Override
	public AgentResponse call(AgentTaskRequest request) {
		Path workspace = request.workingDirectory();

		try {
			// Ensure workspace directory exists
			Files.createDirectories(workspace);

			// Build JBang command to invoke hello-world-agent-ai
			List<String> command = new ArrayList<>();
			command.add("jbang");
			command.add("/home/mark/community/spring-ai-agents/jbang/launcher.java");
			command.add("hello-world-agent-ai");
			command.add("path=hello.txt");
			command.add("content=Hello World!");
			command.add("provider=" + provider);

			// Execute JBang command
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(workspace.toFile());
			pb.redirectErrorStream(true);

			Process process = pb.start();

			// Capture output
			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
			}

			// Wait for completion with timeout
			boolean finished = process.waitFor(5, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				throw new RuntimeException("AI agent execution timed out after 5 minutes");
			}

			int exitCode = process.exitValue();
			String outputStr = output.toString();

			// Check if file was created successfully
			Path helloFile = workspace.resolve("hello.txt");
			boolean fileExists = Files.exists(helloFile);

			if (exitCode == 0 && fileExists) {
				// Success case
				String fileContent = Files.readString(helloFile);
				var metadata = new AgentGenerationMetadata("SUCCESS",
						Map.of("file_created", helloFile.toString(), "content", fileContent, "success", true,
								"provider", provider, "exit_code", exitCode, "output", outputStr));

				var generation = new AgentGeneration(String
					.format("Successfully created hello.txt with AI agent (%s): %s", provider, fileContent.trim()),
						metadata);

				return new AgentResponse(List.of(generation));
			}
			else {
				// Failure case
				var metadata = new AgentGenerationMetadata("ERROR",
						Map.of("error", "AI agent failed or file not created", "success", false, "provider", provider,
								"exit_code", exitCode, "output", outputStr, "file_exists", fileExists));

				var generation = new AgentGeneration(String.format(
						"Failed to create hello.txt with AI agent (%s): exit code %d", provider, exitCode), metadata);

				return new AgentResponse(List.of(generation));
			}

		}
		catch (Exception e) {
			// Create failure generation
			var metadata = new AgentGenerationMetadata("ERROR",
					Map.of("error", e.getMessage(), "success", false, "provider", provider));

			var generation = new AgentGeneration(
					String.format("Failed to execute AI agent (%s): %s", provider, e.getMessage()), metadata);

			return new AgentResponse(List.of(generation));
		}
	}

	@Override
	public boolean isAvailable() {
		// Check if JBang is available and spring-ai-agents is built locally
		try {
			ProcessBuilder pb = new ProcessBuilder("jbang", "--version");
			Process process = pb.start();
			boolean finished = process.waitFor(5, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				return false;
			}

			boolean jbangAvailable = process.exitValue() == 0;

			// Check if spring-ai-agents launcher exists
			Path launcherPath = Path.of("/home/mark/community/spring-ai-agents/jbang/launcher.java");
			boolean launcherExists = Files.exists(launcherPath);

			return jbangAvailable && launcherExists;
		}
		catch (Exception e) {
			return false;
		}
	}

}