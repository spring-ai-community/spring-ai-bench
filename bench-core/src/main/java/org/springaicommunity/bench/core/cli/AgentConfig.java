package org.springaicommunity.bench.core.cli;

import java.util.List;

/**
 * Agent configuration from run YAML
 */
public class AgentConfig {

	private final String type;

	private final String alias;

	private final List<String> args;

	public AgentConfig(String type, String alias, List<String> args) {
		this.type = type;
		this.alias = alias;
		this.args = args;
	}

	public String getType() {
		return type;
	}

	public String getAlias() {
		return alias;
	}

	public List<String> getArgs() {
		return args;
	}

}