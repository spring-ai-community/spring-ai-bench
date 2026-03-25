package org.springaicommunity.bench.core.agent;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Agent configuration loaded from YAML. Defines the command to run and the timeout. The
 * agent is launched in the workspace directory where INSTRUCTION.md has been written.
 */
public class ExecAgentInvoker {

	private final String command;

	private final Duration timeout;

	public ExecAgentInvoker(String command, Duration timeout) {
		this.command = command;
		this.timeout = timeout;
	}

	public String command() {
		return command;
	}

	public Duration timeout() {
		return timeout;
	}

	/**
	 * Loads agent config from a YAML file. Minimal format:
	 *
	 * <pre>
	 * command: claude
	 * timeout: PT45M
	 * </pre>
	 */
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

		return new ExecAgentInvoker(command, timeout);
	}

}
