package org.springaicommunity.bench.core.cli;

import java.nio.file.Paths;

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
		System.out.println("  run      --benchmark <name> [--agent <config>] [--task <id>]");
		System.out.println("           Run provide + agent + grade end-to-end");
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

		JudgeFactory factory = new JudgeFactory();
		// Register test-quality-llm as a pass-through until agent-bench-agents provides
		// the real LLM judge. This keeps the cascade functional for T0-T2.
		factory.register("test-quality-llm",
				config -> ctx -> org.springaicommunity.judge.result.Judgment.abstain("LLM judge not configured"));

		new RunCommand(Paths.get("benchmarks"), factory).run(benchmark, agent, task);
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
