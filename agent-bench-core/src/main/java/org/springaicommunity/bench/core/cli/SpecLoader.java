package org.springaicommunity.bench.core.cli;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and merges Case and Run specifications according to the genesis plan.
 */
public class SpecLoader {

	private final Yaml yaml = new Yaml();

	public RunSpec loadRunSpec(BenchMain.RunConfig config) throws IOException {
		CaseSpec caseSpec;
		RunYaml runYaml = null;

		// 1. Load run YAML if specified
		if (config.getRunFile() != null) {
			runYaml = loadRunYaml(config.getRunFile());
			String caseId = runYaml.getCaseId();
			if (caseId == null && config.getCaseId() == null) {
				throw new IllegalArgumentException("Case ID must be specified in run file or --case argument");
			}
			if (config.getCaseId() == null) {
				config.setCaseId(caseId);
			}
		}

		// 2. Load case YAML
		if (config.getCaseId() == null) {
			throw new IllegalArgumentException("Case ID is required (use --case or specify in run file)");
		}
		caseSpec = loadCaseSpec(config.getCaseId());

		// 3. Merge configurations (case → run → CLI overrides)
		return mergeSpecs(caseSpec, runYaml, config);
	}

	private CaseSpec loadCaseSpec(String caseId) throws IOException {
		Path casePath = Paths.get("bench-tracks").resolve(caseId).resolve("cases").resolve(caseId + ".yaml");
		if (!Files.exists(casePath)) {
			throw new IllegalArgumentException("Case not found: " + casePath);
		}

		try (FileInputStream fis = new FileInputStream(casePath.toFile())) {
			Map<String, Object> data = yaml.load(fis);
			return mapToCaseSpec(data);
		}
	}

	private RunYaml loadRunYaml(Path runFile) throws IOException {
		if (!Files.exists(runFile)) {
			throw new IllegalArgumentException("Run file not found: " + runFile);
		}

		try (FileInputStream fis = new FileInputStream(runFile.toFile())) {
			Map<String, Object> data = yaml.load(fis);
			return mapToRunYaml(data);
		}
	}

	private RunSpec mergeSpecs(CaseSpec caseSpec, RunYaml runYaml, BenchMain.RunConfig config) {
		// Start with case defaults
		Map<String, Object> inputs = new HashMap<>(caseSpec.getInputs());
		// TODO: Update to use Judge configuration instead of VerifierSpec
		// For now, verifier configuration is handled directly in AgentModelAdapter
		Object verifier = null;

		// Apply run YAML overrides
		if (runYaml != null) {
			if (runYaml.getInputs() != null) {
				inputs.putAll(runYaml.getInputs());
			}
			if (runYaml.getVerifier() != null) {
				verifier = runYaml.getVerifier(); // Full replacement for now
			}
		}

		// Apply CLI overrides
		if (config.getInputOverrides() != null) {
			inputs.putAll(config.getInputOverrides());
		}

		// Determine run ID
		String runId = config.getRunId();
		if ("auto".equals(runId)) {
			runId = UUID.randomUUID().toString();
		}

		// Get agent configuration from run YAML or create default
		AgentConfig agentConfig = (runYaml != null && runYaml.getAgent() != null) ? runYaml.getAgent()
				: new AgentConfig("jbang", "hello-world", null);

		// Determine output directory
		Path outputDir = config.getOutputDir().resolve(runId);

		return new RunSpec(caseSpec.getId(), caseSpec.getDescription(), runId, outputDir, agentConfig, inputs, verifier,
				config.isForce());
	}

	private CaseSpec mapToCaseSpec(Map<String, Object> data) {
		String id = (String) data.get("id");
		String description = (String) data.get("description");

		@SuppressWarnings("unchecked")
		Map<String, Object> inputs = (Map<String, Object>) data.get("inputs");
		if (inputs == null)
			inputs = new HashMap<>();

		@SuppressWarnings("unchecked")
		Map<String, Object> verifierData = (Map<String, Object>) data.get("verifier");
		// TODO: Convert to Judge configuration
		Object verifier = null;

		return new CaseSpec(id, description, inputs, verifier);
	}

	private RunYaml mapToRunYaml(Map<String, Object> data) {
		String caseId = (String) data.get("case");

		@SuppressWarnings("unchecked")
		Map<String, Object> inputs = (Map<String, Object>) data.get("inputs");

		@SuppressWarnings("unchecked")
		Map<String, Object> agentData = (Map<String, Object>) data.get("agent");
		AgentConfig agent = null;
		if (agentData != null) {
			String type = (String) agentData.get("type");
			String alias = (String) agentData.get("alias");
			@SuppressWarnings("unchecked")
			java.util.List<String> args = (java.util.List<String>) agentData.get("args");
			agent = new AgentConfig(type, alias, args);
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> verifierData = (Map<String, Object>) data.get("verifier");
		// TODO: Convert to Judge configuration
		Object verifier = null;

		return new RunYaml(caseId, inputs, agent, verifier);
	}

	// TODO: Implement mapToJudgeSpec when updating configuration system
	// private Judge mapToJudgeSpec(Map<String, Object> judgeData) { ... }

}