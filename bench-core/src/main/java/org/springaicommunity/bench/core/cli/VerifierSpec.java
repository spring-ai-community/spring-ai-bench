package org.springaicommunity.bench.core.cli;

import java.util.List;

/**
 * Verifier specification with list of checks
 */
public class VerifierSpec {

	private final List<CheckSpec> checks;

	public VerifierSpec(List<CheckSpec> checks) {
		this.checks = checks;
	}

	public List<CheckSpec> getChecks() {
		return checks;
	}

}