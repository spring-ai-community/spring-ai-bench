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
package org.springaicommunity.bench.agents.claudecode;

import org.springaicommunity.agents.model.*;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simplified Claude Agent Model that directly calls Claude CLI using zt-exec.
 * This bypasses the complex SDK and uses a direct approach for reliable integration.
 */
public class SimplifiedClaudeAgentModel implements AgentModel {

    private final String claudeCliPath;
    private final Duration timeout;

    public SimplifiedClaudeAgentModel() {
        this("claude", Duration.ofMinutes(2));
    }

    public SimplifiedClaudeAgentModel(String claudeCliPath, Duration timeout) {
        this.claudeCliPath = claudeCliPath;
        this.timeout = timeout;
    }

    @Override
    public AgentResponse call(AgentTaskRequest request) {
        Instant startTime = Instant.now();

        try {
            // Execute Claude CLI directly with zt-exec
            ProcessResult result = new ProcessExecutor()
                .command(claudeCliPath, request.goal(), "--dangerously-skip-permissions")
                .directory(request.workingDirectory().toFile())
                .readOutput(true)
                .timeout(timeout.toSeconds(), TimeUnit.SECONDS)
                .execute();

            Duration duration = Duration.between(startTime, Instant.now());

            if (result.getExitValue() == 0) {
                // Success
                AgentGenerationMetadata metadata = new AgentGenerationMetadata("SUCCESS",
                    Map.of("exitCode", result.getExitValue(), "success", true));
                AgentGeneration generation = new AgentGeneration(result.outputUTF8(), metadata);

                AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
                    .model("claude-sonnet-4-0")
                    .duration(duration)
                    .sessionId("")
                    .providerFields(Map.of("cli_exit_code", result.getExitValue()))
                    .build();

                return new AgentResponse(List.of(generation), responseMetadata);
            } else {
                // Failure
                AgentGenerationMetadata metadata = new AgentGenerationMetadata("ERROR",
                    Map.of("exitCode", result.getExitValue(), "success", false));
                AgentGeneration generation = new AgentGeneration(
                    "Claude CLI failed with exit code " + result.getExitValue() + ": " + result.outputUTF8(),
                    metadata);

                AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
                    .model("claude-sonnet-4-0")
                    .duration(duration)
                    .sessionId("")
                    .providerFields(Map.of("cli_exit_code", result.getExitValue()))
                    .build();

                return new AgentResponse(List.of(generation), responseMetadata);
            }

        } catch (Exception e) {
            // Handle execution errors
            Duration duration = Duration.between(startTime, Instant.now());

            AgentGenerationMetadata metadata = new AgentGenerationMetadata("ERROR",
                Map.of("error", e.getMessage(), "success", false));
            AgentGeneration generation = new AgentGeneration("Execution failed: " + e.getMessage(), metadata);

            AgentResponseMetadata responseMetadata = AgentResponseMetadata.builder()
                .model("claude-sonnet-4-0")
                .duration(duration)
                .sessionId("")
                .build();

            return new AgentResponse(List.of(generation), responseMetadata);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            ProcessResult result = new ProcessExecutor()
                .command(claudeCliPath, "--version")
                .readOutput(true)
                .timeout(10, TimeUnit.SECONDS)
                .execute();
            return result.getExitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}