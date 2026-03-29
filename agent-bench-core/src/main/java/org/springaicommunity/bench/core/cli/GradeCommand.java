package org.springaicommunity.bench.core.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springaicommunity.bench.core.benchmark.Benchmark;
import org.springaicommunity.bench.core.benchmark.BenchmarkCatalog;
import org.springaicommunity.bench.core.benchmark.BenchmarkTask;
import org.springaicommunity.bench.core.benchmark.JudgeFactory;
import org.springaicommunity.bench.core.result.FailureMode;
import org.springaicommunity.bench.core.result.TrialResult;
import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;

/**
 * Handles the {@code grade} CLI command. Evaluates an agent's workspace using the
 * benchmark's jury configuration.
 */
public class GradeCommand {

	private final BenchmarkCatalog catalog;

	private final JudgeFactory judgeFactory;

	private final ObjectMapper jsonMapper = new ObjectMapper();

	public GradeCommand(Path benchmarksDir) {
		this.catalog = new BenchmarkCatalog(benchmarksDir);
		this.judgeFactory = new JudgeFactory();
	}

	public GradeCommand() {
		this(Paths.get("benchmarks"));
	}

	/**
	 * Grades an agent's workspace for a specific benchmark task.
	 * @param benchmarkName the benchmark name
	 * @param taskId the item ID
	 * @param workspace the workspace to evaluate
	 */
	public TrialResult grade(String benchmarkName, String taskId, Path workspace) throws IOException {
		Benchmark benchmark = findBenchmark(benchmarkName);
		BenchmarkTask task = findTask(benchmark, taskId);

		// Build judge from benchmark jury config
		Judge judge = judgeFactory.createFromConfig(benchmark.juryConfig());

		// Evaluate
		JudgmentContext context = JudgmentContext.builder().workspace(workspace).goal(task.instruction()).build();

		TrialResult result;
		try {
			Judgment judgment = judge.judge(context);
			boolean resolved = judgment.pass();
			result = new TrialResult(task.id(), resolved, Map.of("judgment", judgment.reasoning()),
					resolved ? FailureMode.NONE : FailureMode.TEST_FAILURE, null, 0, workspace);
		}
		catch (Exception e) {
			result = new TrialResult(task.id(), false, Map.of("error", e.getMessage()), FailureMode.GRADE_ERROR, null, 0,
					workspace);
		}

		// Output result as JSON
		String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
		System.out.println(json);

		return result;
	}

	private Benchmark findBenchmark(String name) throws IOException {
		List<Benchmark> benchmarks = catalog.discover();
		return benchmarks.stream()
			.filter(b -> b.name().equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Benchmark not found: " + name));
	}

	private BenchmarkTask findTask(Benchmark benchmark, String taskId) {
		if (taskId == null && benchmark.tasks().size() == 1) {
			return benchmark.tasks().get(0);
		}
		if (taskId == null) {
			throw new IllegalArgumentException("Item ID required for multi-item benchmarks");
		}
		return benchmark.tasks()
			.stream()
			.filter(i -> i.id().equals(taskId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Item not found: " + taskId));
	}

}
