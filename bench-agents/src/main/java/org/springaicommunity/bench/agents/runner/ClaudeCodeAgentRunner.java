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
package org.springaicommunity.bench.agents.runner;

import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.bench.agents.runner.AgentModelAdapter;
import org.springaicommunity.bench.agents.verifier.SuccessVerifier;
import org.springaicommunity.bench.core.run.AgentResult;
import org.springaicommunity.bench.core.run.AgentRunner;
import org.springaicommunity.bench.core.spec.AgentSpec;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Agent runner implementation for Claude Code CLI agent.
 * Wraps ClaudeCodeAgentModel with the AgentModelAdapter to bridge
 * between Spring AI Bench and Spring AI Agents APIs.
 */
public class ClaudeCodeAgentRunner implements AgentRunner {

    private final AgentModelAdapter adapter;

    public ClaudeCodeAgentRunner(ClaudeCodeAgentModel model, SuccessVerifier verifier) {
        this.adapter = new AgentModelAdapter(model, verifier);
    }

    @Override
    public AgentResult run(Path workspace, AgentSpec spec, Duration timeout) throws Exception {
        return adapter.run(workspace, spec, timeout);
    }
}