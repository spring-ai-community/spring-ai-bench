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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springaicommunity.bench.core.agent.ExecAgentInvoker;
import org.springaicommunity.bench.core.benchmark.Benchmark;
import org.springaicommunity.bench.core.benchmark.BenchmarkCatalog;
import org.springaicommunity.bench.core.benchmark.BenchmarkItem;
import org.springaicommunity.bench.core.benchmark.JudgeFactory;
import org.springaicommunity.bench.core.result.BenchmarkResult;
import org.springaicommunity.bench.core.result.FailureMode;
import org.springaicommunity.bench.core.result.ItemResult;
import org.springaicommunity.judge.Judge;
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

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	public RunCommand(Path benchmarksDir) {
		this.catalog = new BenchmarkCatalog(benchmarksDir);
		this.judgeFactory = new JudgeFactory();
		this.provideCommand = new ProvideCommand(benchmarksDir);
	}

	public RunCommand() {
		this(Paths.get("benchmarks"));
	}

	/**
	 * Runs a complete benchmark with a specified agent.
	 * @param benchmarkName the benchmark name
	 * @param agentConfig path to agent config YAML, or null for manual mode
	 * @param itemFilter optional item ID to run only a specific item
	 */
	public BenchmarkResult run(String benchmarkName, String agentConfig, String itemFilter) throws Exception {
		Benchmark benchmark = findBenchmark(benchmarkName);
		String runId = UUID.randomUUID().toString();
		Path runDir = Paths.get("runs", runId);

		// Filter items if requested
		List<BenchmarkItem> items = benchmark.items();
		if (itemFilter != null) {
			items = items.stream().filter(i -> i.id().equals(itemFilter)).toList();
			if (items.isEmpty()) {
				throw new IllegalArgumentException("Item not found: " + itemFilter);
			}
		}

		// Write run metadata
		Files.createDirectories(runDir);
		Map<String, Object> runMeta = Map.of("benchmark", benchmarkName, "agent",
				agentConfig != null ? agentConfig : "manual", "timestamp", Instant.now().toString(), "runId", runId);
		yamlMapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("run.yaml").toFile(), runMeta);

		// Load agent invoker if config provided
		ExecAgentInvoker invoker = agentConfig != null ? ExecAgentInvoker.fromYaml(Paths.get(agentConfig)) : null;

		// Build judge from benchmark config
		Judge judge = judgeFactory.createFromConfig(benchmark.juryConfig());

		Instant runStart = Instant.now();
		List<ItemResult> results = new ArrayList<>();

		for (BenchmarkItem item : items) {
			System.out.printf("Running item: %s%n", item.id());
			ItemResult result = runItem(benchmark, item, judge, invoker, runDir, runId);
			results.add(result);

			// Write individual result
			Path itemDir = runDir.resolve("items").resolve(item.id());
			Files.createDirectories(itemDir);
			jsonMapper.writerWithDefaultPrettyPrinter().writeValue(itemDir.resolve("result.json").toFile(), result);

			System.out.printf("  %s: %s%n", item.id(), result.resolved() ? "RESOLVED" : "FAILED");
		}

		Duration totalDuration = Duration.between(runStart, Instant.now());
		BenchmarkResult benchmarkResult = BenchmarkResult.fromItems(benchmarkName, benchmark.version(), runId,
				invoker != null ? invoker.name() : "manual", results, totalDuration, 0.0);

		// Write aggregate result
		jsonMapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("result.json").toFile(), benchmarkResult);

		System.out.println();
		System.out.printf("Benchmark: %s%n", benchmarkName);
		System.out.printf("Accuracy: %.1f%% (%d/%d)%n", benchmarkResult.accuracy() * 100,
				results.stream().filter(ItemResult::resolved).count(), results.size());
		System.out.printf("Duration: %s%n", totalDuration);
		System.out.printf("Results: %s%n", runDir.resolve("result.json"));

		return benchmarkResult;
	}

	private ItemResult runItem(Benchmark benchmark, BenchmarkItem item, Judge judge, ExecAgentInvoker invoker,
			Path runDir, String runId) {
		Path workspace = runDir.resolve("items").resolve(item.id()).resolve("workspace");

		try {
			// Provide: set up workspace
			provideCommand.provide(benchmark.name(), item.id(), workspace);

			// Invoke agent (if configured)
			Instant agentStart = Instant.now();
			if (invoker != null) {
				Duration timeout = item.timeout() != null ? item.timeout() : benchmark.defaultTimeout();
				invoker.invoke(item.instruction(), workspace, timeout);
			}
			else {
				System.out.println("  No agent configured. Grade workspace manually.");
				return new ItemResult(item.id(), false, Map.of(), FailureMode.AGENT_ERROR, Duration.ZERO, 0, workspace);
			}
			Duration agentDuration = Duration.between(agentStart, Instant.now());

			// Grade: evaluate result
			JudgmentContext context = JudgmentContext.builder()
				.workspace(workspace)
				.goal(item.instruction())
				.executionTime(agentDuration)
				.startedAt(agentStart)
				.status(ExecutionStatus.SUCCESS)
				.build();

			Judgment judgment = judge.judge(context);
			boolean resolved = judgment.pass();

			return new ItemResult(item.id(), resolved, Map.of("reasoning", judgment.reasoning()),
					resolved ? FailureMode.NONE : FailureMode.TEST_FAILURE, agentDuration, 0, workspace);
		}
		catch (Exception e) {
			FailureMode mode = FailureMode.AGENT_ERROR;
			if (e.getMessage() != null && e.getMessage().contains("timed out")) {
				mode = FailureMode.TIMEOUT;
			}
			return new ItemResult(item.id(), false, Map.of("error", e.getMessage()), mode, null, 0, workspace);
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
