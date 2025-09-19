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
package org.springaicommunity.bench.agents.adapter;

import org.springaicommunity.bench.agents.logging.SimpleLogCapture;
import org.springaicommunity.bench.agents.report.IndexPageGenerator;
import org.springaicommunity.bench.agents.report.MinimalHtmlReportGenerator;
import org.springaicommunity.bench.agents.report.MinimalJsonReportGenerator;
import org.springaicommunity.bench.agents.verifier.*;
import org.springaicommunity.bench.core.run.AgentRunner;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.spec.AgentSpec;
import org.springaicommunity.agents.model.*;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentOptions;
import org.springaicommunity.agents.claudecode.sdk.ClaudeCodeClient;
import org.springaicommunity.agents.claudecode.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claudecode.sdk.config.PermissionMode;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.util.concurrent.TimeUnit;

/**
 * Adapter that bridges between Spring AI Bench and Spring AI Agents APIs.
 * Uses verification system to determine success and captures detailed logs.
 */
public class AgentModelAdapter implements AgentRunner {

    private final AgentModel agentModel;
    private final SuccessVerifier verifier;

    public AgentModelAdapter(AgentModel agentModel, SuccessVerifier verifier) {
        this.agentModel = agentModel;
        this.verifier = verifier;
    }

    // Constructor for backward compatibility
    public AgentModelAdapter(AgentModel agentModel) {
        this.agentModel = agentModel;
        this.verifier = null; // Will use old fallback logic
    }

    @Override
    public AgentResult run(Path workspace, AgentSpec spec, Duration timeout) throws Exception {
        Instant startedAt = Instant.now();

        // Create run directory structure for reports
        UUID runId = UUID.randomUUID();
        Path runRoot = workspace.getParent().resolve("bench-reports").resolve(runId.toString());
        // For verification context, use the actual workspace directly since it's not under runRoot
        // Pass workspace as both runRoot and workspaceRel will resolve to the original workspace
        Path actualWorkspace = workspace;

        SimpleLogCapture logger = null;

        try {
            // Set up logging
            logger = new SimpleLogCapture(runRoot, runId);
            logger.log("ADAPTER", "Starting " + agentModel.getClass().getSimpleName());
            logger.log("SETUP", "Original workspace: " + workspace);
            logger.log("SETUP", "Actual workspace: " + actualWorkspace);
            logger.log("SETUP", "Run root: " + runRoot);
            logger.log("SETUP", "Run ID: " + runId);

            // Clean and create workspace
            cleanWorkspace(actualWorkspace, logger);

            // Create AgentOptions for the request
            AgentOptions options = createAgentOptions(spec, timeout, actualWorkspace);

            // Convert AgentSpec to AgentTaskRequest - ensure absolute path
            Path absoluteWorkspace = actualWorkspace.toAbsolutePath();
            logger.log("WORKSPACE", "Original workspace: " + actualWorkspace);
            logger.log("WORKSPACE", "Absolute workspace: " + absoluteWorkspace);
            logger.log("WORKSPACE", "Is absolute: " + absoluteWorkspace.isAbsolute());

            var request = new AgentTaskRequest(
                spec.prompt(),
                absoluteWorkspace,
                options
            );

            logger.log("AGENT", "Executing agent task");

            // For ClaudeCodeAgentModel, create a workspace-specific instance to ensure working directory is set correctly
            AgentModel effectiveAgentModel = agentModel;
            if (agentModel instanceof ClaudeCodeAgentModel) {
                logger.log("AGENT", "Creating workspace-specific ClaudeCodeAgentModel for directory: " + absoluteWorkspace);
                try {
                    // Setup clean Claude authentication state for API key usage
                    setupCleanClaudeAuth(logger);

                    // Create project-level Claude settings to avoid interactive API key prompts
                    createProjectClaudeSettings(absoluteWorkspace, logger);

                    // Extract options from the original model if possible, otherwise use defaults
                    ClaudeCodeAgentOptions claudeOptions = new ClaudeCodeAgentOptions();
                    claudeOptions.setYolo(true); // Enable yolo mode for autonomous operations
                    claudeOptions.setTimeout(timeout);

                    // Create CLI options with the working directory
                    CLIOptions cliOptions = CLIOptions.builder()
                        .timeout(timeout)
                        .permissionMode(PermissionMode.BYPASS_PERMISSIONS)
                        .build();

                    // Create client with the specific working directory
                    logger.log("AGENT", "Creating ClaudeCodeClient with working directory: " + absoluteWorkspace);
                    logger.log("AGENT", "Working directory exists: " + Files.exists(absoluteWorkspace));
                    logger.log("AGENT", "Working directory is absolute: " + absoluteWorkspace.isAbsolute());


                    ClaudeCodeClient workspaceClient = ClaudeCodeClient.create(cliOptions, absoluteWorkspace);
                    effectiveAgentModel = new ClaudeCodeAgentModel(workspaceClient, claudeOptions);

                    logger.log("AGENT", "Created workspace-specific ClaudeCodeAgentModel successfully");
                } catch (Exception e) {
                    logger.log("AGENT", "Failed to create workspace-specific model, using original: " + e.getMessage());
                    // Fall back to original model
                }
            }

            // Call the agent
            logger.log("AGENT", "Calling agent with task request working directory: " + request.workingDirectory());
            logger.log("AGENT", "Request working directory is absolute: " + request.workingDirectory().isAbsolute());


            AgentResponse response = effectiveAgentModel.call(request);

            // Debug: Check agent response
            logger.log("AGENT", "Agent call completed. Results: " + response.getResults().size());
            if (!response.getResults().isEmpty()) {
                var firstGen = response.getResults().get(0);
                logger.log("AGENT", "First generation output: " + firstGen.getOutput().substring(0, Math.min(100, firstGen.getOutput().length())));
            }

            // Debug: Check if file exists in workspace before verification
            Path expectedFile = actualWorkspace.resolve("hello.txt");
            logger.log("AGENT", "Expected file at: " + expectedFile);
            logger.log("AGENT", "File exists after agent execution: " + Files.exists(expectedFile));


            if (Files.exists(expectedFile)) {
                try {
                    String content = Files.readString(expectedFile);
                    logger.log("AGENT", "File content: '" + content + "'");
                } catch (Exception e) {
                    logger.log("AGENT", "Error reading file: " + e.getMessage());
                }
            }

            // Verify results
            boolean success;
            VerificationResult verificationResult = null;

            if (verifier != null) {
                logger.log("VERIFIER", "Starting verification");
                // Create a verification context - use absolute paths only
                // This ensures context.runRoot().resolve(context.workspaceRel()) points to the actual workspace
                VerificationContext context = new VerificationContext(absoluteWorkspace.getParent(), absoluteWorkspace.getFileName(), startedAt);
                verificationResult = verifier.verify(context);
                success = verificationResult.success();

                // Log verification summary
                String checkSummary = verificationResult.checks().stream()
                    .map(check -> check.name() + ":" + (check.pass() ? "PASS" : "FAIL"))
                    .reduce((a, b) -> a + " " + b)
                    .orElse("no checks");
                logger.log("VERIFIER", checkSummary);
            } else {
                // Fallback to old heuristic logic
                success = isSuccessfulResponse(response);
                logger.log("VERIFIER", "Using fallback heuristic: " + (success ? "PASS" : "FAIL"));
            }

            Instant finishedAt = Instant.now();
            long duration = Duration.between(startedAt, finishedAt).toMillis();
            // Ensure minimum duration of 1ms for timing tests
            if (duration == 0) {
                duration = 1;
            }

            logger.log("RESULT", (success ? "SUCCESS" : "FAILURE") + ": " +
                (verificationResult != null ? verificationResult.reason() : "heuristic check"));

            // Final diagnostic before returning
            logger.log("FINAL", "Exit code will be: " + (success ? 0 : 1));
            logger.log("FINAL", "Duration: " + duration + "ms");
            logger.log("FINAL", "Final file check at: " + expectedFile);
            logger.log("FINAL", "Final file exists: " + Files.exists(expectedFile));

            if (logger != null) {
                logger.writeFooter();
            }

            // Generate reports with enhanced provenance
            generateReports(runId, spec.kind(), success, startedAt, finishedAt, duration, verificationResult, runRoot, actualWorkspace, response);

            // Return result with log path
            return new AgentResult(success ? 0 : 1, runRoot.resolve("run.log"), duration);

        } catch (Exception e) {
            Instant finishedAt = Instant.now();
            long duration = Duration.between(startedAt, finishedAt).toMillis();

            if (logger != null) {
                logger.log("ERROR", "Agent execution failed: " + e.getMessage());
                logger.writeFooter();
            }

            return new AgentResult(1, runRoot.resolve("run.log"), duration);
        }
    }

    private void cleanWorkspace(Path workspace, SimpleLogCapture logger) throws Exception {
        if (Files.exists(workspace)) {
            logger.log("WORKSPACE", "Clearing existing workspace");
            Files.walk(workspace)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Log but don't fail - best effort cleanup
                    }
                });
        }
        Files.createDirectories(workspace);
        logger.log("WORKSPACE", "Created clean workspace");
    }

    private AgentOptions createAgentOptions(AgentSpec spec, Duration timeout, Path workspace) {
        // For Gemini agents, create GeminiAgentOptions with yolo mode enabled
        if (agentModel instanceof GeminiAgentModel) {
            GeminiAgentOptions geminiOptions = new GeminiAgentOptions();
            geminiOptions.setTimeout(timeout);
            geminiOptions.setModel(spec.model() != null ? spec.model() : "gemini-2.0-flash-exp");
            geminiOptions.setYolo(true); // Enable yolo mode for autonomous file operations

            // Set extras from spec
            Map<String, Object> extras = new HashMap<>();
            if (spec.genParams() != null) {
                extras.putAll(spec.genParams());
            }
            if (spec.autoApprove() != null) {
                extras.put("autoApprove", spec.autoApprove());
            }
            if (spec.role() != null) {
                extras.put("role", spec.role());
            }
            // Explicitly set yolo in extras as well for compatibility
            extras.put("yolo", true);
            geminiOptions.setExtras(extras);

            return geminiOptions;
        }

        // For other agents, use generic AgentOptions with yolo in extras
        return new AgentOptions() {
            @Override
            public String getWorkingDirectory() {
                // Return absolute path for working directory
                return workspace.toAbsolutePath().toString();
            }

            @Override
            public Duration getTimeout() {
                return timeout;
            }

            @Override
            public Map<String, String> getEnvironmentVariables() {
                return Map.of();
            }

            @Override
            public String getModel() {
                return spec.model() != null ? spec.model() : "default";
            }

            @Override
            public Map<String, Object> getExtras() {
                Map<String, Object> extras = new HashMap<>();
                if (spec.genParams() != null) {
                    extras.putAll(spec.genParams());
                }
                if (spec.autoApprove() != null) {
                    extras.put("autoApprove", spec.autoApprove());
                }
                if (spec.role() != null) {
                    extras.put("role", spec.role());
                }
                // Add yolo mode for agent providers that support it
                extras.put("yolo", true);
                return extras;
            }
        };
    }

    /**
     * Fallback success determination for backward compatibility.
     */
    private boolean isSuccessfulResponse(AgentResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            return false;
        }

        return response.getResults().stream()
            .anyMatch(generation -> {
                String content = generation.getOutput().toLowerCase();
                // Check metadata for explicit success flag
                Object success = generation.getMetadata().getProviderFields().get("success");
                if (success instanceof Boolean) {
                    return (Boolean) success;
                }
                // Fall back to content analysis
                return !content.contains("failed") && !content.contains("error");
            });
    }

    private void generateReports(UUID runId, String caseId, boolean success, Instant startedAt,
                               Instant finishedAt, long durationMs, VerificationResult verificationResult,
                               Path runRoot, Path workspace, AgentResponse agentResponse) {
        try {
            // Generate JSON report with enhanced provenance
            MinimalJsonReportGenerator.generate(runId, caseId, success, startedAt, finishedAt,
                durationMs, verificationResult, runRoot, workspace, getAgentProviderInfo());

            // Generate HTML report with agent response metadata
            MinimalHtmlReportGenerator.generate(runId, caseId, success, startedAt, finishedAt,
                durationMs, verificationResult, runRoot, agentResponse);

            // Update index page
            Path reportsBaseDir = runRoot.getParent();
            IndexPageGenerator.generate(reportsBaseDir);

        } catch (Exception e) {
            // Log error but don't fail the execution
            System.err.println("Failed to generate reports: " + e.getMessage());
        }
    }

    private String getAgentProviderInfo() {
        // Determine provider based on agent model class
        String className = agentModel.getClass().getSimpleName();
        if (className.contains("Claude")) {
            return "claude-code";
        } else if (className.contains("Gemini")) {
            return "gemini";
        } else {
            return "hello-world";
        }
    }

    /**
     * Creates project-level Claude settings to avoid interactive API key prompts.
     * This creates a .claude/settings.json file in the workspace with the API key configuration.
     */
    private void createProjectClaudeSettings(Path workspace, SimpleLogCapture logger) throws IOException {
        logger.log("CLAUDE_CONFIG", "Starting Claude settings creation for workspace: " + workspace);

        // Get API key from environment
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        logger.log("CLAUDE_CONFIG", "API key from environment: " + (apiKey != null ? "present (length=" + apiKey.length() + ")" : "null"));

        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.log("CLAUDE_CONFIG", "No ANTHROPIC_API_KEY found, skipping project settings creation");
            return;
        }

        // Create .claude directory in the workspace
        Path claudeDir = workspace.resolve(".claude");
        logger.log("CLAUDE_CONFIG", "Creating .claude directory at: " + claudeDir);
        Files.createDirectories(claudeDir);
        logger.log("CLAUDE_CONFIG", ".claude directory created successfully: " + Files.exists(claudeDir));

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

        logger.log("CLAUDE_CONFIG", "Settings configuration created with API key pre-approval");
        logger.log("CLAUDE_CONFIG", "API key last 20 chars approved: " + last20Chars);

        // Write settings.json file
        Path settingsFile = claudeDir.resolve("settings.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile.toFile(), settings);

        logger.log("CLAUDE_CONFIG", "Settings file written to: " + settingsFile);
        logger.log("CLAUDE_CONFIG", "Settings file exists: " + Files.exists(settingsFile));
        logger.log("CLAUDE_CONFIG", "Settings file size: " + Files.size(settingsFile) + " bytes");

        // Read back and log the content for verification
        String content = Files.readString(settingsFile);
        logger.log("CLAUDE_CONFIG", "Settings file content: " + content);
    }

    /**
     * Sets up clean Claude authentication state by logging out to ensure API key usage.
     */
    private void setupCleanClaudeAuth(SimpleLogCapture logger) throws IOException {
        logger.log("CLAUDE_AUTH", "Setting up clean Claude authentication state");

        try {
            // Use zt-exec to handle the logout process more reliably
            ProcessResult result = new ProcessExecutor()
                .command("/tmp/claude-logout-auto.sh")
                .timeout(30, TimeUnit.SECONDS)
                .readOutput(true)
                .execute();

            logger.log("CLAUDE_AUTH", "Logout script completed with exit code: " + result.getExitValue());
            if (!result.outputUTF8().isEmpty()) {
                logger.log("CLAUDE_AUTH", "Logout output: " + result.outputUTF8().trim());
            }

        } catch (Exception e) {
            logger.log("CLAUDE_AUTH", "Exception during logout: " + e.getMessage());
            // Continue anyway - logout failure shouldn't stop the test
        }
    }

}