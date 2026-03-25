package org.springaicommunity.bench.core.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Invokes an external agent process based on YAML configuration. Template variables
 * {@code ${instruction}} and {@code ${workspace}} are substituted at runtime.
 */
public class ExecAgentInvoker {

	private final String name;

	private final List<String> commandTemplate;

	private final String workingDirTemplate;

	private final Duration defaultTimeout;

	public ExecAgentInvoker(String name, List<String> commandTemplate, String workingDirTemplate,
			Duration defaultTimeout) {
		this.name = name;
		this.commandTemplate = List.copyOf(commandTemplate);
		this.workingDirTemplate = workingDirTemplate;
		this.defaultTimeout = defaultTimeout;
	}

	public String name() {
		return name;
	}

	/**
	 * Invokes the agent with the given instruction in the specified workspace.
	 * @param instruction the task instruction
	 * @param workspace the agent's working directory
	 * @param timeout maximum execution time
	 * @throws IOException if the process fails to start
	 * @throws InterruptedException if the process is interrupted
	 */
	public void invoke(String instruction, Path workspace, Duration timeout) throws IOException, InterruptedException {
		Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;

		// Substitute template variables
		List<String> command = new ArrayList<>();
		for (String part : commandTemplate) {
			command.add(part.replace("${instruction}", instruction)
				.replace("${workspace}", workspace.toAbsolutePath().toString()));
		}

		// Determine working directory
		Path workDir = workspace;
		if (workingDirTemplate != null) {
			workDir = Path.of(workingDirTemplate.replace("${workspace}", workspace.toAbsolutePath().toString()));
		}

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workDir.toFile());
		pb.redirectErrorStream(true);

		Process process = pb.start();

		// Capture output
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("  [agent] " + line);
			}
		}

		boolean finished = process.waitFor(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new RuntimeException("Agent execution timed out after " + effectiveTimeout);
		}

		int exitCode = process.exitValue();
		if (exitCode != 0) {
			throw new RuntimeException("Agent exited with code " + exitCode);
		}
	}

	/**
	 * Loads an agent invoker from a YAML configuration file.
	 */
	@SuppressWarnings("unchecked")
	public static ExecAgentInvoker fromYaml(Path configPath) throws IOException {
		ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
		Map<String, Object> config = yamlMapper.readValue(configPath.toFile(), Map.class);

		String name = (String) config.getOrDefault("name", configPath.getFileName().toString());
		List<String> command = (List<String>) config.get("command");
		if (command == null || command.isEmpty()) {
			throw new IllegalArgumentException("Agent config must specify 'command'");
		}
		String workingDir = (String) config.get("working-dir");
		String timeoutStr = (String) config.getOrDefault("timeout", "PT10M");
		Duration timeout = Duration.parse(timeoutStr);

		return new ExecAgentInvoker(name, command, workingDir, timeout);
	}

}
