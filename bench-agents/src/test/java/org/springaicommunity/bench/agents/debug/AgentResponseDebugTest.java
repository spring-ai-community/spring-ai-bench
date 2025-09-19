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

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Debug test to examine AgentResponse structure and metadata
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class AgentResponseDebugTest {

    private Path tempWorkspace;
    private ClaudeCodeAgentModel agentModel;

    @BeforeEach
    void setUp() throws Exception {
        tempWorkspace = Files.createTempDirectory("agent-response-debug-");

        ClaudeCodeAgentOptions options = new ClaudeCodeAgentOptions();
        options.setYolo(true);
        options.setTimeout(Duration.ofMinutes(2));
        options.setModel("claude-sonnet-4-20250514"); // Claude 4 Sonnet

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
    void debugAgentResponseStructure() throws Exception {
        System.out.println("=== DEBUGGING AGENT RESPONSE STRUCTURE ===");
        System.out.println("Workspace: " + tempWorkspace);

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
                return "claude-sonnet-4-20250514";
            }

            @Override
            public Map<String, Object> getExtras() {
                return Map.of("yolo", true);
            }
        };

        // Create task request
        AgentTaskRequest request = new AgentTaskRequest(
            "Create a file named debug-test.txt with contents: Debug Response Test",
            tempWorkspace,
            options
        );

        System.out.println("Calling Claude agent...");

        // Call the agent
        AgentResponse response = agentModel.call(request);

        System.out.println("\n=== AGENT RESPONSE ANALYSIS ===");
        System.out.println("Response class: " + response.getClass().getName());
        System.out.println("Results count: " + response.getResults().size());

        // Analyze metadata
        System.out.println("\n=== RESPONSE METADATA ===");
        var metadata = response.getMetadata();
        System.out.println("Metadata class: " + metadata.getClass().getName());
        System.out.println("Model: " + metadata.getModel());
        System.out.println("Duration: " + metadata.getDuration());
        System.out.println("SessionId: " + metadata.getSessionId());

        // Show all metadata fields
        System.out.println("\n=== ALL METADATA FIELDS ===");
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

        // Analyze results
        System.out.println("\n=== AGENT RESULTS ===");
        for (int i = 0; i < response.getResults().size(); i++) {
            var result = response.getResults().get(i);
            System.out.println("Result " + i + ":");
            System.out.println("  Output: " + result.getOutput());
            System.out.println("  Metadata: " + result.getMetadata());
            System.out.println("  Metadata fields:");
            for (Map.Entry<String, Object> entry : result.getMetadata().getProviderFields().entrySet()) {
                System.out.println("    " + entry.getKey() + " = " + entry.getValue());
            }
        }

        // Check workspace files
        System.out.println("\n=== WORKSPACE FILES ===");
        Files.list(tempWorkspace).forEach(file -> {
            try {
                System.out.println("File: " + file.getFileName() + " (" + Files.size(file) + " bytes)");
                if (Files.isRegularFile(file) && Files.size(file) < 1000) {
                    String content = Files.readString(file);
                    System.out.println("  Content: '" + content + "'");
                }
            } catch (Exception e) {
                System.out.println("  Error reading: " + e.getMessage());
            }
        });
    }
}