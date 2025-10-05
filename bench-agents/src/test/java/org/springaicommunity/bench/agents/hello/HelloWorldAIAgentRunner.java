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

import org.springaicommunity.bench.agents.runner.AgentModelAdapter;
import org.springaicommunity.bench.agents.judge.HelloWorldJudge;

/**
 * Specific agent runner for the Hello World AI agent test. Extends the generic
 * AgentModelAdapter with a HelloWorldAIAgentModel that uses Claude Code or Gemini via
 * JBang integration with spring-ai-agents.
 */
public class HelloWorldAIAgentRunner extends AgentModelAdapter {

	public HelloWorldAIAgentRunner() {
		super(new HelloWorldAIAgentModel(), new HelloWorldJudge());
	}

	public HelloWorldAIAgentRunner(String provider) {
		super(new HelloWorldAIAgentModel(provider), new HelloWorldJudge());
	}

}