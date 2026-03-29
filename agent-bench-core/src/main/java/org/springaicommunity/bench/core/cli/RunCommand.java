package org.springaicommunity.bench.core.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springaicommunity.bench.core.agent.ExecAgentInvoker;
import org.springaicommunity.bench.core.result.AgentJournal;
import org.springaicommunity.bench.core.result.RunMetadata;
import org.springaicommunity.bench.core.benchmark.Benchmark;
import org.springaicommunity.bench.core.benchmark.BenchmarkCatalog;
import org.springaicommunity.bench.core.benchmark.BenchmarkTask;
import org.springaicommunity.bench.core.benchmark.JudgeFactory;
import org.springaicommunity.bench.core.result.BenchmarkResult;
import org.springaicommunity.bench.core.result.FailureMode;
import org.springaicommunity.bench.core.result.TrialResult;
import org.springaicommunity.judge.Judge;
import org.zeroturnaround.exec.ProcessExecutor;
import org.springaicommunity.judge.context.ExecutionStatus;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;

/**
 * Handles the {@code run} CLI command. Orchestrates provide, agent invocation, and grade
 * for each item in a benchmark.
 */
public class RunCommand {

	private final BenchmarkCatalog catalog;

	private final JudgeFactory judgeFactory;

	private final ProvideCommand provideCommand;

	private final ObjectMapper jsonMapper = new ObjectMapper()
		.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	public RunCommand(Path benchmarksDir, JudgeFactory judgeFactory) {
		this.catalog = new BenchmarkCatalog(benchmarksDir);
		this.judgeFactory = judgeFactory;
		this.provideCommand = new ProvideCommand(benchmarksDir);
	}

	public RunCommand(Path benchmarksDir) {
		this(benchmarksDir, new JudgeFactory());
	}

	public RunCommand() {
		this(Paths.get("benchmarks"));
	}

	/**
	 * Runs a complete benchmark with a specified agent.
	 * @param benchmarkName the benchmark name
	 * @param agentConfig path to agent config YAML, or null for manual mode
	 * @param taskFilter optional item ID to run only a specific item
	 */
	public BenchmarkResult run(String benchmarkName, String agentConfig, String taskFilter) throws Exception {
		return run(benchmarkName, agentConfig, taskFilter, 1);
	}

	/**
	 * Runs the benchmark with the specified number of attempts per item. Multiple
	 * attempts enable pass@k computation.
	 */
	public BenchmarkResult run(String benchmarkName, String agentConfig, String taskFilter, int attempts)
			throws Exception {
		Benchmark benchmark = findBenchmark(benchmarkName);
		String runId = UUID.randomUUID().toString();
		Path runDir = Paths.get("runs", runId);

		// Filter items if requested
		List<BenchmarkTask> tasks = benchmark.tasks();
		if (taskFilter != null) {
			tasks = tasks.stream().filter(i -> i.id().equals(taskFilter)).toList();
			if (tasks.isEmpty()) {
				throw new IllegalArgumentException("Task not found: " + taskFilter);
			}
		}

		// Write run metadata and lock file
		Files.createDirectories(runDir);
		RunMetadata runMetadata = RunMetadata.start(runId, benchmarkName, benchmark.version(),
				agentConfig != null ? agentConfig : "manual", attempts);
		jsonMapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("run-metadata.json").toFile(),
				runMetadata);
		jsonMapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("bench.lock").toFile(), runMetadata);

		// Load agent invoker if config provided
		ExecAgentInvoker invoker = agentConfig != null ? ExecAgentInvoker.fromYaml(Paths.get(agentConfig)) : null;

		// Build judge from benchmark config
		Judge judge = judgeFactory.createFromConfig(benchmark.juryConfig());

		Instant runStart = Instant.now();
		List<TrialResult> results = new ArrayList<>();

		for (BenchmarkTask task : tasks) {
			for (int attempt = 1; attempt <= attempts; attempt++) {
				String trialName = attempts > 1 ? task.id() + "__attempt-" + attempt : task.id();
				System.out.printf("Running: %s%n", trialName);
				TrialResult result = runTrial(benchmark, task, judge, invoker, runDir, runId, trialName);
				results.add(result);

				// Write individual result incrementally
				Path trialDir = runDir.resolve("tasks").resolve(trialName);
				Files.createDirectories(trialDir);
				jsonMapper.writerWithDefaultPrettyPrinter()
					.writeValue(trialDir.resolve("result.json").toFile(), result);

				System.out.printf("  %s: %s%n", trialName, result.resolved() ? "RESOLVED" : "FAILED");
			}
		}

		Duration totalDuration = Duration.between(runStart, Instant.now());
		BenchmarkResult benchmarkResult = BenchmarkResult.fromTrials(benchmarkName, benchmark.version(), runId,
				invoker != null ? invoker.command() : "manual", results, totalDuration, 0.0);

		// Write aggregate result and update metadata
		jsonMapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("result.json").toFile(), benchmarkResult);
		@SuppressWarnings("unchecked")
		Map<Integer, Double> passAtK = (Map<Integer, Double>) benchmarkResult.aggregateScores()
			.getOrDefault("passAtK", Map.of());
		RunMetadata completedMetadata = runMetadata.complete(benchmarkResult.accuracy(), passAtK);
		jsonMapper.writerWithDefaultPrettyPrinter()
			.writeValue(runDir.resolve("run-metadata.json").toFile(), completedMetadata);

		System.out.println();
		System.out.printf("Benchmark: %s%n", benchmarkName);
		System.out.printf("Accuracy: %.1f%% (%d/%d)%n", benchmarkResult.accuracy() * 100,
				results.stream().filter(TrialResult::resolved).count(), results.size());
		if (attempts > 1) {
			passAtK.forEach(
					(k, v) -> System.out.printf("Pass@%d: %.1f%%%n", k, v * 100));
		}
		System.out.printf("Duration: %s%n", totalDuration);
		System.out.printf("Results: %s%n", runDir.resolve("result.json"));

		return benchmarkResult;
	}

	private TrialResult runTrial(Benchmark benchmark, BenchmarkTask task, Judge judge, ExecAgentInvoker invoker,
			Path runDir, String runId, String trialName) {
		Path workspace = runDir.resolve("tasks").resolve(trialName).resolve("workspace");

		try {
			// Provide: set up workspace
			provideCommand.provide(benchmark.name(), task.id(), workspace);

			// Setup scripts (before agent)
			runScripts(task.setup(), workspace, "setup");

			// Write instruction if the item provides one
			String instruction = task.instruction();
			if (instruction != null && !instruction.isBlank()) {
				Files.writeString(workspace.resolve("INSTRUCTION.md"), instruction);
			}

			// Invoke agent
			Instant agentStart = Instant.now();
			if (invoker != null) {
				Duration timeout = task.timeout() != null ? task.timeout() : benchmark.defaultTimeout();
				new ProcessExecutor().command("bash", "-c", invoker.command())
					.directory(workspace.toFile())
					.timeout(timeout.toSeconds(), TimeUnit.SECONDS)
					.redirectErrorStream(true)
					.redirectOutput(System.out)
					.execute();
			}
			else {
				System.out.println("  No agent configured. Grade workspace manually.");
				return new TrialResult(task.id(), false, Map.of(), FailureMode.AGENT_ERROR, Duration.ZERO, 0, workspace);
			}
			Instant agentEnd = Instant.now();
			Duration agentDuration = Duration.between(agentStart, agentEnd);

			// Read journal if the agent produced one
			AgentJournal journal = readJournal(workspace);
			if (journal != null) {
				System.out.printf("  Journal: %d turns, $%.4f, %dms%n", journal.totalTurns(), journal.totalCostUsd(),
						journal.durationMs());
			}

			// Post scripts (after agent, before grading)
			runScripts(task.post(), workspace, "post");

			// Grade: evaluate result
			Instant gradeStart = Instant.now();
			JudgmentContext.Builder contextBuilder = JudgmentContext.builder()
				.workspace(workspace)
				.goal(task.instruction())
				.executionTime(agentDuration)
				.startedAt(agentStart)
				.status(ExecutionStatus.SUCCESS);
			// Pass item metadata through to judges (e.g., baselineCoverage for coverage judges)
			task.metadata().forEach(contextBuilder::metadata);
			JudgmentContext context = contextBuilder.build();

			Judgment judgment = judge.judge(context);
			boolean resolved = judgment.pass();
			Instant gradeEnd = Instant.now();

			Map<String, Object> scores = new java.util.HashMap<>();
			scores.put("reasoning", judgment.reasoning());
			if (journal != null) {
				scores.put("journal", journal);
				scores.put("costUsd", journal.totalCostUsd());
				scores.put("turns", journal.totalTurns());
				scores.put("efficiencyVerified", journal.hasCostData());
			}
			long tokens = journal != null ? journal.totalInputTokens() + journal.totalOutputTokens() : 0;

			return new TrialResult(task.id(), resolved, scores,
					resolved ? FailureMode.NONE : FailureMode.TEST_FAILURE, agentDuration, tokens, workspace,
					agentStart, agentEnd, gradeStart, gradeEnd);
		}
		catch (Exception e) {
			FailureMode mode = FailureMode.AGENT_ERROR;
			if (e.getMessage() != null && e.getMessage().contains("timed out")) {
				mode = FailureMode.AGENT_TIMEOUT;
			}
			return new TrialResult(task.id(), false, Map.of("error", e.getMessage()), mode, null, 0, workspace);
		}
	}

	private AgentJournal readJournal(Path workspace) {
		Path journalPath = workspace.resolve("journal.yaml");
		if (!Files.isRegularFile(journalPath)) {
			return null;
		}
		try {
			return yamlMapper.readValue(journalPath.toFile(), AgentJournal.class);
		}
		catch (IOException e) {
			System.err.println("  Warning: failed to parse journal.yaml: " + e.getMessage());
			return null;
		}
	}

	private void runScripts(List<String> scripts, Path workspace, String phase) throws Exception {
		if (scripts == null || scripts.isEmpty()) {
			return;
		}
		for (String script : scripts) {
			System.out.printf("  [%s] %s%n", phase, script);
			new ProcessExecutor().command("bash", "-c", script)
				.directory(workspace.toFile())
				.timeout(5, TimeUnit.MINUTES)
				.redirectErrorStream(true)
				.redirectOutput(System.out)
				.execute();
		}
	}

	private Benchmark findBenchmark(String name) throws IOException {
		List<Benchmark> benchmarks = catalog.discover();
		return benchmarks.stream()
			.filter(b -> b.name().equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Benchmark not found: " + name));
	}

}
