package org.springaicommunity.bench.core.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.Judges;
import org.springaicommunity.judge.fs.FileContentJudge;
import org.springaicommunity.judge.fs.FileExistsJudge;

/**
 * CLI entry point for Spring AI Bench. Dispatches to command handlers for the new
 * benchmark architecture (list, items, provide, grade, run) and legacy run support.
 */
public class BenchMain {

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				printUsage();
				System.exit(1);
			}

			String command = args[0];
			switch (command) {
				case "list" -> new ListCommand().listBenchmarks();
				case "items" -> handleItemsCommand(args);
				case "provide" -> handleProvideCommand(args);
				case "grade" -> handleGradeCommand(args);
				case "run" -> handleRunOrLegacyRun(args);
				default -> {
					System.err.println("Unknown command: " + command);
					printUsage();
					System.exit(1);
				}
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
		System.out.println("  list                            List available benchmarks");
		System.out.println("  items    --benchmark <name>     List items in a benchmark");
		System.out.println("  provide  --benchmark <name> --item <id> --workspace <dir>");
		System.out.println("           Set up workspace for an agent");
		System.out.println("  grade    --benchmark <name> --item <id> --workspace <dir>");
		System.out.println("           Evaluate agent results");
		System.out.println("  run      --benchmark <name> [--agent <config>] [--item <id>]");
		System.out.println("           Run provide + agent + grade end-to-end");
		System.out.println();
		System.out.println("Legacy run options:");
		System.out.println("  run      --run-file <path>      Run from legacy run YAML");
		System.out.println("  run      --case <id>            Run legacy case");
	}

	private static void handleItemsCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		if (benchmark == null) {
			throw new IllegalArgumentException("--benchmark is required");
		}
		new ListCommand().listItems(benchmark);
	}

	private static void handleProvideCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		String item = parseArg(args, "--item");
		String workspace = parseArg(args, "--workspace");
		if (benchmark == null || workspace == null) {
			throw new IllegalArgumentException("--benchmark and --workspace are required");
		}
		new ProvideCommand().provide(benchmark, item, Paths.get(workspace));
	}

	private static void handleGradeCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		String item = parseArg(args, "--item");
		String workspace = parseArg(args, "--workspace");
		if (benchmark == null || workspace == null) {
			throw new IllegalArgumentException("--benchmark and --workspace are required");
		}
		new GradeCommand().grade(benchmark, item, Paths.get(workspace));
	}

	private static void handleRunOrLegacyRun(String[] args) throws Exception {
		// Detect if this is a legacy run (--run-file or --case) or new-style
		// (--benchmark)
		String benchmark = parseArg(args, "--benchmark");
		if (benchmark != null) {
			String agent = parseArg(args, "--agent");
			String item = parseArg(args, "--item");
			new RunCommand().run(benchmark, agent, item);
		}
		else {
			handleLegacyRunCommand(args);
		}
	}

	private static void handleLegacyRunCommand(String[] args) throws Exception {
		// Parse run command arguments
		RunConfig config = parseLegacyRunArgs(args);

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

	static String parseArg(String[] args, String name) {
		for (int i = 0; i < args.length - 1; i++) {
			if (name.equals(args[i])) {
				return args[i + 1];
			}
		}
		return null;
	}

	private static RunConfig parseLegacyRunArgs(String[] args) {
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
