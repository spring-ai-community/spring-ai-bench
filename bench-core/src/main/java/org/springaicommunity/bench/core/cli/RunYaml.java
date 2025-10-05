package org.springaicommunity.bench.core.cli;

import java.util.Map;

/**
 * Represents a loaded run YAML file (runs/examples/*.yaml)
 */
public class RunYaml {

	private final String caseId;

	private final Map<String, Object> inputs;

	private final AgentConfig agent;

	private final Object verifier;

	public RunYaml(String caseId, Map<String, Object> inputs, AgentConfig agent, Object verifier) {
		this.caseId = caseId;
		this.inputs = inputs;
		this.agent = agent;
		this.verifier = verifier;
	}

	public String getCaseId() {
		return caseId;
	}

	public Map<String, Object> inputs() {
		return inputs;
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public AgentConfig getAgent() {
		return agent;
	}

	public Object getVerifier() {
		return verifier;
	}

}