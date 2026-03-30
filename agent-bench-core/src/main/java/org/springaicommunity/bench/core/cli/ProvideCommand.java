package org.springaicommunity.bench.core.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springaicommunity.bench.core.benchmark.Benchmark;
import org.springaicommunity.bench.core.benchmark.BenchmarkCatalog;
import org.springaicommunity.bench.core.benchmark.BenchmarkTask;

/**
 * Handles the {@code provide} CLI command. Sets up a workspace for an agent by copying
 * the workspace template and writing INSTRUCTION.md and .bench-context.yaml.
 */
public class ProvideCommand {

	private final BenchmarkCatalog catalog;

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	public ProvideCommand(Path benchmarksDir) {
		this.catalog = new BenchmarkCatalog(benchmarksDir);
	}

	public ProvideCommand() {
		this(Paths.get("benchmarks"));
	}

	/**
	 * Provides a workspace for a specific benchmark task.
	 * @param benchmarkName the benchmark name
	 * @param taskId the item ID (null for single-item benchmarks)
	 * @param workspace the target workspace directory
	 */
	public void provide(String benchmarkName, String taskId, Path workspace) throws IOException {
		Benchmark benchmark = findBenchmark(benchmarkName);
		BenchmarkTask task = findTask(benchmark, taskId);

		// Create workspace directory
		Files.createDirectories(workspace);

		// Copy workspace template if it exists
		if (task.workspaceTemplate() != null && Files.isDirectory(task.workspaceTemplate())) {
			copyDirectory(task.workspaceTemplate(), workspace);
		}

		// Write INSTRUCTION.md
		Files.writeString(workspace.resolve("INSTRUCTION.md"), task.instruction());

		// Write .bench-context.yaml
		Map<String, Object> context = Map.of("benchmark", benchmarkName, "task", task.id(), "version",
				benchmark.version(), "timeout",
				task.timeout() != null ? task.timeout().toString() : benchmark.defaultTimeout().toString());
		yamlMapper.writerWithDefaultPrettyPrinter()
			.writeValue(workspace.resolve(".bench-context.yaml").toFile(), context);

		System.out.printf("Workspace prepared at: %s%n", workspace);
		System.out.printf("  Benchmark: %s%n", benchmarkName);
		System.out.printf("  Task: %s%n", task.id());
		System.out.println("  Read INSTRUCTION.md for the task description.");
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
			throw new IllegalArgumentException("Task ID required for benchmarks with multiple tasks. "
					+ "Use 'bench tasks --benchmark " + benchmark.name() + "' to see available tasks.");
		}
		return benchmark.tasks()
			.stream()
			.filter(i -> i.id().equals(taskId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
	}

	private void copyDirectory(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path targetDir = target.resolve(source.relativize(dir));
				Files.createDirectories(targetDir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				// Skip .gitkeep files
				if (file.getFileName().toString().equals(".gitkeep")) {
					return FileVisitResult.CONTINUE;
				}
				Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
