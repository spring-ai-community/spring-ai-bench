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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springaicommunity.agents.claudecode.ClaudeCodeAgentModel;
import org.springaicommunity.agents.gemini.GeminiAgentModel;
import org.springaicommunity.bench.agents.runner.ClaudeCodeAgentRunner;
import org.springaicommunity.bench.agents.runner.GeminiAgentRunner;
import org.springaicommunity.bench.agents.verifier.SuccessVerifier;
import org.springaicommunity.bench.core.run.AgentRunner;

/**
 * Auto-configuration for agent providers.
 * Conditionally creates agent runners based on available dependencies and configuration.
 */
@Configuration
public class AgentProviderConfig {

    @Bean
    @ConditionalOnClass(ClaudeCodeAgentModel.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    @ConditionalOnProperty(name = "spring.ai.bench.agent.provider", havingValue = "claude-code")
    public AgentRunner claudeRunner(ClaudeCodeAgentModel model, SuccessVerifier verifier) {
        return new ClaudeCodeAgentRunner(model, verifier);
    }

    @Bean
    @ConditionalOnClass(GeminiAgentModel.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    @ConditionalOnProperty(name = "spring.ai.bench.agent.provider", havingValue = "gemini")
    public AgentRunner geminiRunner(GeminiAgentModel model, SuccessVerifier verifier) {
        return new GeminiAgentRunner(model, verifier);
    }
}