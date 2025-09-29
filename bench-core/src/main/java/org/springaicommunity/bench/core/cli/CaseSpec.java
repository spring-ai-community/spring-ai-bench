package org.springaicommunity.bench.core.cli;

import java.util.Map;

/**
 * Represents a loaded case specification from bench-tracks/.../cases/<case>.yaml
 */
public class CaseSpec {

	private final String id;

	private final String description;

	private final Map<String, Object> inputs;

	private final VerifierSpec verifier;

	public CaseSpec(String id, String description, Map<String, Object> inputs, VerifierSpec verifier) {
		this.id = id;
		this.description = description;
		this.inputs = inputs;
		this.verifier = verifier;
	}

	public String getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, Object> getInputs() {
		return inputs;
	}

	public VerifierSpec getVerifier() {
		return verifier;
	}

}