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
import org.springaicommunity.bench.agents.claudecode.ClaudeCodeAgentRunner;
import org.springaicommunity.bench.agents.hello.HelloWorldVerifier;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for real ClaudeCodeAgentModel execution through bench-agents.
 * This test verifies the complete pipeline with actual Claude Code calls.
 */
@Tag("agents-live")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@Timeout(180)  // Cap runtime at 3 minutes for Claude execution
class ClaudeCodeAgentModelIntegrationTest {

    private Path tempWorkspace;
    private ClaudeCodeAgentRunner agentRunner;

    @BeforeAll
    static void requireClaudeCode() {
        // Check if Claude CLI is available
        assumeTrue(isCliAvailable("claude"), "Claude CLI not available on PATH");
    }

    @BeforeEach
    void setUp() throws Exception {
        tempWorkspace = Files.createTempDirectory("claude-model-test-");

        // Create ClaudeCodeAgentModel with default configuration
        ClaudeCodeClient client = ClaudeCodeClient.create();
        ClaudeCodeAgentOptions options = new ClaudeCodeAgentOptions();
        options.setYolo(true); // Skip permissions for testing

        ClaudeCodeAgentModel agentModel = new ClaudeCodeAgentModel(client, options);
        assumeTrue(agentModel.isAvailable(), "ClaudeCodeAgentModel not available");

        agentRunner = new ClaudeCodeAgentRunner(agentModel, new HelloWorldVerifier());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempWorkspace != null && Files.exists(tempWorkspace)) {
            Files.walk(tempWorkspace)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
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
    void claudeCodeAgentModel_integration_pipeline_works() throws Exception {
        // Create AgentSpec for hello world task
        AgentSpec spec = new AgentSpec(
            "hello-world",      // kind
            null,              // model - will use default
            true,              // autoApprove - skip permissions
            "Create a file named hello.txt in the current working directory with EXACT contents: Hello World!", // prompt
            null,              // genParams
            null               // role
        );

        // Run the agent through the full pipeline
        AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

        // Verify the result
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.logFile()).exists();
        assertThat(result.durationMillis()).isGreaterThan(0); // Should take some time with real Claude

        // Verify the hello.txt file was created with correct content
        Path helloFile = tempWorkspace.resolve("hello.txt");
        assertThat(helloFile).exists();

        String content = Files.readString(helloFile);
        assertThat(content.trim()).isEqualTo("Hello World!");

        System.out.println("✅ SUCCESS: ClaudeCodeAgentModel integration pipeline works end-to-end");
    }

    @Test
    void claudeCodeAgentModel_report_generation_with_provenance() throws Exception {
        // Create AgentSpec
        AgentSpec spec = new AgentSpec(
            "hello-world",       // kind
            "claude-sonnet-4-0", // specific model
            true,               // autoApprove
            "Create hello.txt with exact content: Hello World!", // prompt
            null,               // genParams
            null                // role
        );

        // Run the agent
        AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

        // Verify reports were generated
        Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
        assertThat(reportsDir).exists();

        // Find the most recent run directory
        Path runDir = Files.list(reportsDir)
            .filter(Files::isDirectory)
            .sorted((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (Exception e) {
                    return 0;
                }
            })
            .findFirst()
            .orElseThrow(() -> new AssertionError("No run directory found"));

        // Verify report files exist
        Path jsonReport = runDir.resolve("report.json");
        Path htmlReport = runDir.resolve("index.html");
        Path logFile = runDir.resolve("run.log");

        assertThat(jsonReport).exists();
        assertThat(htmlReport).exists();
        assertThat(logFile).exists();

        // Verify JSON report contains Claude-specific provenance
        String jsonContent = Files.readString(jsonReport);
        assertThat(jsonContent).contains("\"success\" : true");
        assertThat(jsonContent).contains("\"caseId\" : \"hello-world\"");
        assertThat(jsonContent).contains("\"provenance\"");
        assertThat(jsonContent).contains("\"benchVersion\"");

        // Verify agent-specific provenance
        assertThat(jsonContent).contains("\"provider\" : \"claude-code\"");
        assertThat(jsonContent).contains("\"agentsVersion\"");

        // Verify HTML report contains expected structure
        String htmlContent = Files.readString(htmlReport);
        assertThat(htmlContent).contains("Agent Execution Report");
        assertThat(htmlContent).contains("SUCCESS");
        assertThat(htmlContent).contains("Verification Checks");

        System.out.println("✅ SUCCESS: ClaudeCodeAgentModel report generation with proper provenance");
    }

    @Test
    void claudeCodeAgentModel_verification_system_works() throws Exception {
        // Test with a scenario that should pass verification
        AgentSpec spec = new AgentSpec(
            "hello-world",  // kind
            null,          // model
            true,          // autoApprove
            "Create hello.txt with exact content: Hello World!", // prompt
            null,          // genParams
            null           // role
        );

        AgentResult result = agentRunner.run(tempWorkspace, spec, Duration.ofMinutes(2));

        // Should succeed because ClaudeCodeAgentModel should create the correct file
        assertThat(result.exitCode()).isEqualTo(0);

        // Verify the verification checks passed
        Path reportsDir = tempWorkspace.getParent().resolve("bench-reports");
        Path runDir = Files.list(reportsDir)
            .filter(Files::isDirectory)
            .sorted((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (Exception e) {
                    return 0;
                }
            })
            .findFirst()
            .orElseThrow();

        String jsonContent = Files.readString(runDir.resolve("report.json"));
        assertThat(jsonContent).contains("\"success\" : true");
        assertThat(jsonContent).contains("\"pass\" : true"); // Verification checks passed

        System.out.println("✅ SUCCESS: ClaudeCodeAgentModel verification system correctly validates agent output");
    }

    private static boolean isCliAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "--version")
                .redirectErrorStream(true)
                .start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}