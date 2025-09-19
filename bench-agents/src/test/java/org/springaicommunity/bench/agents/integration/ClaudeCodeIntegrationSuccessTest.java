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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.AgentOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration success test demonstrating that Claude Code integration works.
 * This test focuses on core functionality rather than verification logic.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeCodeIntegrationSuccessTest {

    private Path tempWorkspace;
    private ClaudeCodeAgentModel agentModel;

    @BeforeEach
    void setUp() throws Exception {
        tempWorkspace = Files.createTempDirectory("claude-success-test-");

        // Create ClaudeCodeAgentModel
        ClaudeCodeAgentOptions options = new ClaudeCodeAgentOptions();
        options.setYolo(true);
        options.setTimeout(Duration.ofMinutes(2));

        ClaudeCodeClient client = ClaudeCodeClient.create(
            org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions.builder()
                .timeout(Duration.ofMinutes(2))
                .permissionMode(org.springaicommunity.agents.claudecode.sdk.config.PermissionMode.BYPASS_PERMISSIONS)
                .build(),
            tempWorkspace
        );

        agentModel = new ClaudeCodeAgentModel(client, options);
        assumeTrue(agentModel.isAvailable(), "ClaudeCodeAgentModel not available");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempWorkspace != null && Files.exists(tempWorkspace)) {
            Files.walk(tempWorkspace)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // Best effort cleanup
                    }
                });
        }
    }

    @Test
    void claudeCode_integration_works_successfully() throws Exception {
        System.out.println("Testing Claude Code integration in workspace: " + tempWorkspace);

        // Create AgentOptions
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

        // Create task request
        AgentTaskRequest request = new AgentTaskRequest(
            "Create a file named test-success.txt with EXACT contents: Integration Success!",
            tempWorkspace,
            options
        );

        System.out.println("Calling Claude agent...");

        // Call the agent
        AgentResponse response = agentModel.call(request);

        System.out.println("Agent response received");
        System.out.println("Results count: " + response.getResults().size());

        // Verify agent execution succeeded
        assertThat(response).isNotNull();
        assertThat(response.getResults()).isNotEmpty();

        // Verify file was created immediately after agent execution
        Path testFile = tempWorkspace.resolve("test-success.txt");
        assertThat(testFile).exists();

        String content = Files.readString(testFile);
        assertThat(content).isEqualTo("Integration Success!");

        // Verify response metadata contains Claude information
        assertThat(response.getMetadata()).isNotNull();
        assertThat(response.getMetadata().getModel()).contains("claude");

        System.out.println("✅ SUCCESS: Claude Code integration works!");
        System.out.println("Model: " + response.getMetadata().getModel());
        System.out.println("Duration: " + response.getMetadata().getDuration());
        System.out.println("File created: " + testFile);
        System.out.println("File content: '" + content + "'");
    }
}