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
import org.springaicommunity.bench.core.benchmark.BenchmarkItem;

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
	 * Provides a workspace for a specific benchmark item.
	 * @param benchmarkName the benchmark name
	 * @param itemId the item ID (null for single-item benchmarks)
	 * @param workspace the target workspace directory
	 */
	public void provide(String benchmarkName, String itemId, Path workspace) throws IOException {
		Benchmark benchmark = findBenchmark(benchmarkName);
		BenchmarkItem item = findItem(benchmark, itemId);

		// Create workspace directory
		Files.createDirectories(workspace);

		// Copy workspace template if it exists
		if (item.workspaceTemplate() != null && Files.isDirectory(item.workspaceTemplate())) {
			copyDirectory(item.workspaceTemplate(), workspace);
		}

		// Write INSTRUCTION.md
		Files.writeString(workspace.resolve("INSTRUCTION.md"), item.instruction());

		// Write .bench-context.yaml
		Map<String, Object> context = Map.of("benchmark", benchmarkName, "item", item.id(), "version",
				benchmark.version(), "timeout",
				item.timeout() != null ? item.timeout().toString() : benchmark.defaultTimeout().toString());
		yamlMapper.writerWithDefaultPrettyPrinter()
			.writeValue(workspace.resolve(".bench-context.yaml").toFile(), context);

		System.out.printf("Workspace prepared at: %s%n", workspace);
		System.out.printf("  Benchmark: %s%n", benchmarkName);
		System.out.printf("  Item: %s%n", item.id());
		System.out.println("  Read INSTRUCTION.md for the task description.");
	}

	private Benchmark findBenchmark(String name) throws IOException {
		List<Benchmark> benchmarks = catalog.discover();
		return benchmarks.stream()
			.filter(b -> b.name().equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Benchmark not found: " + name));
	}

	private BenchmarkItem findItem(Benchmark benchmark, String itemId) {
		if (itemId == null && benchmark.items().size() == 1) {
			return benchmark.items().get(0);
		}
		if (itemId == null) {
			throw new IllegalArgumentException("Item ID required for benchmarks with multiple items. "
					+ "Use 'bench items --benchmark " + benchmark.name() + "' to see available items.");
		}
		return benchmark.items()
			.stream()
			.filter(i -> i.id().equals(itemId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
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
