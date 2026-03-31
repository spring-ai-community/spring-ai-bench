package org.springaicommunity.bench.core.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springaicommunity.bench.core.benchmark.JudgeFactory;

/**
 * CLI entry point for Agent Bench. Dispatches to command handlers for the benchmark
 * architecture (list, items, provide, grade, run).
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
				case "tasks" -> handleTasksCommand(args);
				case "provide" -> handleProvideCommand(args);
				case "grade" -> handleGradeCommand(args);
				case "run" -> handleRunCommand(args);
				case "resume" -> handleResumeCommand(args);
				case "compare" -> handleCompareCommand(args);
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
		System.out.println("  tasks    --benchmark <name>     List tasks in a benchmark");
		System.out.println("  provide  --benchmark <name> --task <id> --workspace <dir>");
		System.out.println("           Set up workspace for an agent");
		System.out.println("  grade    --benchmark <name> --task <id> --workspace <dir>");
		System.out.println("           Evaluate agent results");
		System.out.println("  run      --benchmark <name> [--agent <config>] [--task <id>] [--difficulty <level>]");
		System.out.println("           Run provide + agent + grade end-to-end");
		System.out.println("  resume   --run-id <uuid>     Resume an interrupted run");
		System.out.println("  compare  --runs <dir1> <dir2> ...  Compare benchmark results");
	}

	private static void handleTasksCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		if (benchmark == null) {
			throw new IllegalArgumentException("--benchmark is required");
		}
		new ListCommand().listTasks(benchmark);
	}

	private static void handleProvideCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		String task = parseArg(args, "--task");
		String workspace = parseArg(args, "--workspace");
		if (benchmark == null || workspace == null) {
			throw new IllegalArgumentException("--benchmark and --workspace are required");
		}
		new ProvideCommand().provide(benchmark, task, Paths.get(workspace));
	}

	private static void handleGradeCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		String task = parseArg(args, "--task");
		String workspace = parseArg(args, "--workspace");
		if (benchmark == null || workspace == null) {
			throw new IllegalArgumentException("--benchmark and --workspace are required");
		}
		new GradeCommand().grade(benchmark, task, Paths.get(workspace));
	}

	private static void handleRunCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		if (benchmark == null) {
			throw new IllegalArgumentException("--benchmark is required");
		}
		String agent = parseArg(args, "--agent");
		String task = parseArg(args, "--task");
		String difficulty = parseArg(args, "--difficulty");

		JudgeFactory factory = new JudgeFactory();
		// Register test-quality-llm as a pass-through until agent-bench-agents provides
		// the real LLM judge. This keeps the cascade functional for T0-T2.
		factory.register("test-quality-llm",
				config -> ctx -> org.springaicommunity.judge.result.Judgment.abstain("LLM judge not configured"));

		new RunCommand(Paths.get("benchmarks"), factory).run(benchmark, agent, task, 1, difficulty);
	}

	private static void handleResumeCommand(String[] args) throws Exception {
		String runId = parseArg(args, "--run-id");
		if (runId == null) {
			throw new IllegalArgumentException("--run-id is required");
		}
		JudgeFactory factory = new JudgeFactory();
		factory.register("test-quality-llm",
				config -> ctx -> org.springaicommunity.judge.result.Judgment.abstain("LLM judge not configured"));
		new RunCommand(Paths.get("benchmarks"), factory).resume(runId);
	}

	private static void handleCompareCommand(String[] args) throws Exception {
		List<Path> runDirs = parseRunDirs(args);
		if (runDirs.isEmpty()) {
			throw new IllegalArgumentException("--runs requires at least one directory");
		}
		new CompareCommand().compare(runDirs);
	}

	private static List<Path> parseRunDirs(String[] args) {
		List<Path> dirs = new ArrayList<>();
		boolean collecting = false;
		for (int i = 1; i < args.length; i++) {
			if ("--runs".equals(args[i])) {
				collecting = true;
				continue;
			}
			if (collecting) {
				if (args[i].startsWith("--")) {
					break;
				}
				dirs.add(Paths.get(args[i]));
			}
		}
		return dirs;
	}

	static String parseArg(String[] args, String name) {
		for (int i = 0; i < args.length - 1; i++) {
			if (name.equals(args[i])) {
				return args[i + 1];
			}
		}
		return null;
	}

}
