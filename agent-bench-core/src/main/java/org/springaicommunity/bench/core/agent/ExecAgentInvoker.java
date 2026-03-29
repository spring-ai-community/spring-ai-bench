package org.springaicommunity.bench.core.agent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Agent configuration loaded from YAML. Defines the command to run, timeout, and optional
 * setup/post scripts. The agent is launched in the workspace directory.
 *
 * <p>
 * Setup/post scripts live on the agent config (not the benchmark item) because the
 * lifecycle depends on the agent type: a generic agent needs the bench to clone the repo
 * and run tests; a purpose-built agent handles its own workflow internally.
 */
public class ExecAgentInvoker {

	private final String command;

	private final Duration timeout;

	private final List<String> setup;

	private final List<String> post;

	public ExecAgentInvoker(String command, Duration timeout, List<String> setup, List<String> post) {
		this.command = command;
		this.timeout = timeout;
		this.setup = setup != null ? List.copyOf(setup) : List.of();
		this.post = post != null ? List.copyOf(post) : List.of();
	}

	public ExecAgentInvoker(String command, Duration timeout) {
		this(command, timeout, List.of(), List.of());
	}

	public String command() {
		return command;
	}

	public Duration timeout() {
		return timeout;
	}

	public List<String> setup() {
		return setup;
	}

	public List<String> post() {
		return post;
	}

	@SuppressWarnings("unchecked")
	public static ExecAgentInvoker fromYaml(Path configPath) throws IOException {
		ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
		Map<String, Object> config = yamlMapper.readValue(configPath.toFile(), Map.class);

		String command = (String) config.get("command");
		if (command == null || command.isBlank()) {
			throw new IllegalArgumentException("Agent config must specify 'command': " + configPath);
		}
		String timeoutStr = (String) config.getOrDefault("timeout", "PT10M");
		Duration timeout = Duration.parse(timeoutStr);
		List<String> setup = (List<String>) config.getOrDefault("setup", List.of());
		List<String> post = (List<String>) config.getOrDefault("post", List.of());

		return new ExecAgentInvoker(command, timeout, setup, post);
	}

}
