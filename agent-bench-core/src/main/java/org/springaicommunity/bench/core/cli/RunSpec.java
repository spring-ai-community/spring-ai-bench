package org.springaicommunity.bench.core.cli;

import java.nio.file.Path;
import java.util.Map;

/**
 * Complete run specification after merging case + run + CLI overrides
 */
public class RunSpec {

	private final String caseId;

	private final String description;

	private final String runId;

	private final Path outputDir;

	private final AgentConfig agent;

	private final Map<String, Object> inputs;

	private final Object verifier;

	private final boolean force;

	public RunSpec(String caseId, String description, String runId, Path outputDir, AgentConfig agent,
			Map<String, Object> inputs, Object verifier, boolean force) {
		this.caseId = caseId;
		this.description = description;
		this.runId = runId;
		this.outputDir = outputDir;
		this.agent = agent;
		this.inputs = inputs;
		this.verifier = verifier;
		this.force = force;
	}

	public String getCaseId() {
		return caseId;
	}

	public String getDescription() {
		return description;
	}

	public String getRunId() {
		return runId;
	}

	public Path getOutputDir() {
		return outputDir;
	}

	public AgentConfig getAgent() {
		return agent;
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public Object getVerifier() {
		return verifier;
	}

	public boolean isForce() {
		return force;
	}

}