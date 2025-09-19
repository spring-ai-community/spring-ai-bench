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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        System.out.println("Testing Claude Code integration in workspace: " + tempWorkspace);

        // Setup clean Claude authentication state for API key usage
        setupCleanClaudeAuth();

        // Create project-level Claude settings to avoid interactive API key prompts
        createProjectClaudeSettings(tempWorkspace);

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
        // Temporarily disable cleanup for debugging
        System.out.println("🚨 CLEANUP DISABLED - Workspace preserved for manual testing: " + tempWorkspace);
        System.out.println("You can now cd to the workspace and test Claude CLI manually:");
        System.out.println("  cd " + tempWorkspace);
        System.out.println("  claude");

        /*
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
        */
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

    /**
     * Creates project-level Claude settings to avoid interactive API key prompts.
     */
    private void createProjectClaudeSettings(Path workspace) throws IOException {
        System.out.println("Creating project-level Claude settings for workspace: " + workspace);

        // Get API key from environment
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("No ANTHROPIC_API_KEY found, skipping project settings creation");
            return;
        }

        System.out.println("API key found, length: " + apiKey.length());

        // Create .claude directory in the workspace
        Path claudeDir = workspace.resolve(".claude");
        Files.createDirectories(claudeDir);
        System.out.println("Created .claude directory: " + claudeDir);

        // Create settings configuration with API key pre-approval
        Map<String, Object> settings = new HashMap<>();

        // Extract last 20 characters for approval (Claude CLI requirement)
        String last20Chars = apiKey.substring(Math.max(0, apiKey.length() - 20));
        Map<String, Object> customApiKeyResponses = new HashMap<>();
        customApiKeyResponses.put("approved", List.of(last20Chars));
        customApiKeyResponses.put("rejected", List.of());

        Map<String, Object> env = new HashMap<>();
        env.put("ANTHROPIC_API_KEY", apiKey);

        settings.put("hasCompletedOnboarding", true);
        settings.put("customApiKeyResponses", customApiKeyResponses);
        settings.put("env", env);

        System.out.println("API key last 20 chars approved: " + last20Chars);

        // Write settings.json file
        Path settingsFile = claudeDir.resolve("settings.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile.toFile(), settings);

        System.out.println("Created Claude settings file: " + settingsFile);
        System.out.println("Settings file size: " + Files.size(settingsFile) + " bytes");
        System.out.println("Settings content: " + Files.readString(settingsFile));
    }

    /**
     * Sets up clean Claude authentication state by logging out to ensure API key usage.
     */
    private void setupCleanClaudeAuth() throws Exception {
        System.out.println("Setting up clean Claude authentication state");

        try {
            // Logout from any existing Claude session to ensure clean state
            ProcessBuilder logoutPb = new ProcessBuilder("claude", "/logout");
            logoutPb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            logoutPb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process logoutProcess = logoutPb.start();

            // Wait for logout with timeout to avoid hanging
            boolean finished = logoutProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

            if (finished) {
                int logoutCode = logoutProcess.exitValue();
                if (logoutCode == 0) {
                    System.out.println("Successfully logged out from Claude");
                } else {
                    System.out.println("Claude logout returned code: " + logoutCode + " (may already be logged out)");
                }
            } else {
                System.out.println("Claude logout timed out (may already be logged out)");
                logoutProcess.destroyForcibly();
            }

        } catch (Exception e) {
            System.out.println("Failed to logout from Claude (may not be logged in): " + e.getMessage());
            // Continue anyway - this just ensures clean state
        }
    }
}