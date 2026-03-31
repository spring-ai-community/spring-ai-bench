package org.springaicommunity.bench.agents.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springaicommunity.bench.agents.judge.TestQualityJudge;
import org.springaicommunity.bench.core.benchmark.JudgeFactory;
import org.springaicommunity.bench.core.cli.BenchMain;
import org.springaicommunity.bench.core.cli.CompareCommand;
import org.springaicommunity.bench.core.cli.RunCommand;

/**
 * Full-featured CLI entry point with all judges wired — including the LLM-based T3 judge.
 * Use this instead of BenchMain when bench-agents is on the classpath.
 *
 * <p>
 * bench-core's BenchMain registers T3 as an abstain stub. This class registers the real
 * TestQualityJudge backed by ClaudeAgentModel.
 */
public class BenchApp {

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				printUsage();
				System.exit(1);
			}

			String command = args[0];
			switch (command) {
				case "run" -> handleRunCommand(args);
				case "resume" -> handleResumeCommand(args);
				case "compare" -> handleCompareCommand(args);
				default -> {
					// Delegate non-run commands to BenchMain (list, tasks, provide,
					// grade)
					BenchMain.main(args);
				}
			}
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	static JudgeFactory createJudgeFactory() {
		JudgeFactory factory = new JudgeFactory();
		factory.register("test-quality-llm", config -> {
			String promptPath = (String) config.get("prompt");
			String model = (String) config.getOrDefault("model", "claude-sonnet-4-6");
			Duration timeout = Duration.ofMinutes(10);

			Path resolvedPrompt = Path.of(promptPath);
			return new TestQualityJudge(TestQualityJudge.defaultAgentClientFactory(model, timeout), resolvedPrompt);
		});
		return factory;
	}

	private static void handleRunCommand(String[] args) throws Exception {
		String benchmark = parseArg(args, "--benchmark");
		if (benchmark == null) {
			throw new IllegalArgumentException("--benchmark is required");
		}
		String agent = parseArg(args, "--agent");
		String task = parseArg(args, "--task");
		String difficulty = parseArg(args, "--difficulty");

		new RunCommand(Paths.get("benchmarks"), createJudgeFactory()).run(benchmark, agent, task, 1, difficulty);
	}

	private static void handleResumeCommand(String[] args) throws Exception {
		String runId = parseArg(args, "--run-id");
		if (runId == null) {
			throw new IllegalArgumentException("--run-id is required");
		}
		new RunCommand(Paths.get("benchmarks"), createJudgeFactory()).resume(runId);
	}

	private static void handleCompareCommand(String[] args) throws Exception {
		List<Path> runDirs = parseRunDirs(args);
		if (runDirs.isEmpty()) {
			throw new IllegalArgumentException("--runs requires at least one directory");
		}
		new CompareCommand().compare(runDirs);
	}

	private static void printUsage() {
		System.out.println("Usage: bench <command> [options]");
		System.out.println();
		System.out.println("Commands:");
		System.out.println("  list                            List available benchmarks");
		System.out.println("  tasks    --benchmark <name>     List tasks in a benchmark");
		System.out.println("  provide  --benchmark <name> --task <id> --workspace <dir>");
		System.out.println("  grade    --benchmark <name> --task <id> --workspace <dir>");
		System.out.println("  run      --benchmark <name> [--agent <config>] [--task <id>] [--difficulty <level>]");
		System.out.println("  resume   --run-id <uuid>");
		System.out.println("  compare  --runs <dir1> <dir2> ...");
	}

	static String parseArg(String[] args, String name) {
		for (int i = 0; i < args.length - 1; i++) {
			if (name.equals(args[i])) {
				return args[i + 1];
			}
		}
		return null;
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

}
