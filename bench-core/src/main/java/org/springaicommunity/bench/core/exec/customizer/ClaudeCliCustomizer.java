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
package org.springaicommunity.bench.core.exec.customizer;

import org.springaicommunity.bench.core.exec.ExecSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated customizer for Claude CLI agent that injects MCP tools via the --tools flag.
 * <p>
 * This customizer recognizes commands starting with "claude-cli" and automatically
 * appends the appropriate --tools parameter based on the MCP configuration in the ExecSpec.
 * <p>
 * Example transformation:
 * <pre>
 * Original: ["claude-cli", "--run", "agent.py"]
 * With MCP: ["claude-cli", "--run", "agent.py", "--tools=brave,filesystem"]
 * </pre>
 */
public class ClaudeCliCustomizer implements ExecSpecCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeCliCustomizer.class);

    private static final String CLAUDE_CLI_COMMAND = "claude-cli";
    private static final String TOOLS_FLAG_PREFIX = "--tools=";

    @Override
    public ExecSpec customize(ExecSpec original) {
        if (!isClaudeCliCommand(original) || hasNoMcpTools(original)) {
            return original;
        }

        if (hasExistingToolsFlag(original)) {
            throw new IllegalStateException(
                    "ExecSpec command already contains a '--tools' flag, cannot add another.");
        }

        List<String> newCommand = new ArrayList<>(original.command());
        String toolsValue = String.join(",", original.mcp().servers());
        newCommand.add(TOOLS_FLAG_PREFIX + toolsValue);

        logger.debug("Injecting tools flag for claude-cli: {}", toolsValue);

        return original.toBuilder()
                .command(newCommand)
                .build();
    }

    private boolean hasNoMcpTools(ExecSpec spec) {
        boolean hasNoTools = spec.mcp() == null;
        if (hasNoTools) {
            logger.debug("No MCP tools configured for claude-cli command");
        }
        return hasNoTools;
    }

    private boolean hasExistingToolsFlag(ExecSpec spec) {
        return spec.command().stream().anyMatch(arg -> arg.startsWith(TOOLS_FLAG_PREFIX));
    }

    /**
     * Checks if the command is a claude-cli command.
     */
    private boolean isClaudeCliCommand(ExecSpec spec) {
        return CLAUDE_CLI_COMMAND.equals(spec.command().get(0));
    }

    /**
     * Gets the tools flag that would be added for the given spec.
     * Useful for testing and debugging.
     *
     * @param spec the execution spec
     * @return the tools flag that would be added, or null if none
     */
    public String getToolsFlag(ExecSpec spec) {
        if (!isClaudeCliCommand(spec) || spec.mcp() == null) {
            return null;
        }
        return TOOLS_FLAG_PREFIX + String.join(",", spec.mcp().servers());
    }
}