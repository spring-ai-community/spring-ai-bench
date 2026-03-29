package org.springaicommunity.bench.core.cli;

import java.nio.file.Paths;

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
				case "items" -> handleItemsCommand(args);
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
		System.out.println("  items    --benchmark <name>     List items in a benchmark");
		System.out.println("  provide  --benchmark <name> --item <id> --workspace <dir>");
		System.out.println("           Set up workspace for an agent");
		System.out.println("  grade    --benchmark <name> --item <id> --workspace <dir>");
		System.out.println("           Evaluate agent results");
		System.out.println("  run      --benchmark <name> [--agent <config>] [--item <id>]");
		System.out.println("           Run provide + agent + grade end-to-end");
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

	private static void handleRunCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		if (benchmark == null) {
			throw new IllegalArgumentException("--benchmark is required");
		}
		String agent = parseArg(args, "--agent");
		String item = parseArg(args, "--item");
		new RunCommand().run(benchmark, agent, item);
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
