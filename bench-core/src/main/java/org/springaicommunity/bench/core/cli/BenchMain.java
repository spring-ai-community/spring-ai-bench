package org.springaicommunity.bench.core.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.Judges;
import org.springaicommunity.agents.judge.fs.FileContentJudge;
import org.springaicommunity.agents.judge.fs.FileExistsJudge;

/**
 * CLI entry point for Spring AI Bench following the genesis plan. Supports the new
 * YAML-based case/run configuration system.
 */
public class BenchMain {

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				printUsage();
				System.exit(1);
			}

			String command = args[0];
			if ("run".equals(command)) {
				handleRunCommand(args);
			}
			else {
				System.err.println("Unknown command: " + command);
				printUsage();
				System.exit(1);
			}
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void printUsage() {
		System.out.println("Usage: bench <command> [options]");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  run    Run a benchmark");
		System.out.println();
		System.out.println("Run options:");
		System.out.println("  --case <id>                     Case ID to run");
		System.out.println("  --run-file <path>               Path to run YAML file");
		System.out.println("  --run-id <uuid|auto>            Run ID (auto generates UUID)");
		System.out.println("  --out <dir>                     Output directory (default: bench-reports)");
		System.out.println("  --input <key>=<value>           Override input parameter");
		System.out.println("  --force                         Overwrite existing run");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  bench run --run-file runs/examples/hello-world-run.yaml");
		System.out.println("  bench run --case hello-world --run-id test-1 --input content=\"Hello Test!\"");
	}

	private static void handleRunCommand(String[] args) throws Exception {
		// Parse run command arguments
		RunConfig config = parseRunArgs(args);

		// Load and merge specifications
		SpecLoader specLoader = new SpecLoader();
		RunSpec runSpec = specLoader.loadRunSpec(config);

		// Create judges map (manual DI for CLI)
		Map<String, Judge> judges = createJudges();

		// Execute the run
		BenchRunner runner = new BenchRunner(judges);
		runner.execute(runSpec);

		System.out.println("Run completed successfully. Results in: " + runSpec.getOutputDir());
	}

	/**
	 * Create judges for known benchmark cases. In CLI mode, we manually create the judges
	 * that would normally be provided by Spring DI.
	 */
	private static Map<String, Judge> createJudges() {
		Map<String, Judge> judges = new HashMap<>();

		// hello-world judge
		judges.put("hello-world", Judges.allOf(new FileExistsJudge("hello.txt"),
				new FileContentJudge("hello.txt", "Hello World!", FileContentJudge.MatchMode.EXACT)));

		return judges;
	}

	private static RunConfig parseRunArgs(String[] args) {
		RunConfig config = new RunConfig();
		Map<String, String> inputs = new HashMap<>();

		for (int i = 1; i < args.length; i++) {
			String arg = args[i];

			if ("--case".equals(arg) && i + 1 < args.length) {
				config.setCaseId(args[++i]);
			}
			else if ("--run-file".equals(arg) && i + 1 < args.length) {
				config.setRunFile(Paths.get(args[++i]));
			}
			else if ("--run-id".equals(arg) && i + 1 < args.length) {
				config.setRunId(args[++i]);
			}
			else if ("--out".equals(arg) && i + 1 < args.length) {
				config.setOutputDir(Paths.get(args[++i]));
			}
			else if ("--force".equals(arg)) {
				config.setForce(true);
			}
			else if (arg.startsWith("--input") && i + 1 < args.length) {
				String input = args[++i];
				int eq = input.indexOf('=');
				if (eq > 0) {
					String key = input.substring(0, eq);
					String value = input.substring(eq + 1);
					inputs.put(key, value);
				}
				else {
					throw new IllegalArgumentException("Invalid input format: " + input + " (expected key=value)");
				}
			}
			else {
				throw new IllegalArgumentException("Unknown argument: " + arg);
			}
		}

		config.setInputOverrides(inputs);
		return config;
	}

	static class RunConfig {

		private String caseId;

		private Path runFile;

		private String runId = "auto";

		private Path outputDir = Paths.get("bench-reports");

		private boolean force = false;

		private Map<String, String> inputOverrides = new HashMap<>();

		// Getters and setters
		public String getCaseId() {
			return caseId;
		}

		public void setCaseId(String caseId) {
			this.caseId = caseId;
		}

		public Path getRunFile() {
			return runFile;
		}

		public void setRunFile(Path runFile) {
			this.runFile = runFile;
		}

		public String getRunId() {
			return runId;
		}

		public void setRunId(String runId) {
			this.runId = runId;
		}

		public Path getOutputDir() {
			return outputDir;
		}

		public void setOutputDir(Path outputDir) {
			this.outputDir = outputDir;
		}

		public boolean isForce() {
			return force;
		}

		public void setForce(boolean force) {
			this.force = force;
		}

		public Map<String, String> getInputOverrides() {
			return inputOverrides;
		}

		public void setInputOverrides(Map<String, String> inputOverrides) {
			this.inputOverrides = inputOverrides;
		}

	}

}