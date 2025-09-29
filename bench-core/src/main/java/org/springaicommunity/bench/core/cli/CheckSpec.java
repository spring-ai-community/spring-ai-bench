package org.springaicommunity.bench.core.cli;

/**
 * Individual check specification for verifier
 */
public class CheckSpec {

	private final String type;

	private final String path;

	private final String expected;

	public CheckSpec(String type, String path, String expected) {
		this.type = type;
		this.path = path;
		this.expected = expected;
	}

	public String getType() {
		return type;
	}

	public String getPath() {
		return path;
	}

	public String getExpected() {
		return expected;
	}

}