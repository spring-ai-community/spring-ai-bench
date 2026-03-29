package org.springaicommunity.bench.core.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Scans a {@code benchmarks/} directory for {@code benchmark.yaml} files and returns a
 * list of {@link Benchmark} instances.
 */
public class BenchmarkCatalog {

	private final Path benchmarksDir;

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

	public BenchmarkCatalog(Path benchmarksDir) {
		this.benchmarksDir = benchmarksDir;
	}

	public List<Benchmark> discover() throws IOException {
		List<Benchmark> benchmarks = new ArrayList<>();
		if (!Files.isDirectory(benchmarksDir)) {
			return benchmarks;
		}

		try (Stream<Path> dirs = Files.list(benchmarksDir)) {
			for (Path dir : dirs.filter(Files::isDirectory).toList()) {
				Path yamlFile = dir.resolve("benchmark.yaml");
				if (Files.exists(yamlFile)) {
					benchmarks.add(loadBenchmark(dir, yamlFile));
				}
			}
		}
		return benchmarks;
	}

	private Benchmark loadBenchmark(Path benchmarkDir, Path yamlFile) throws IOException {
		BenchmarkYaml yaml = yamlMapper.readValue(yamlFile.toFile(), BenchmarkYaml.class);

		List<BenchmarkTask> items = loadTasks(benchmarkDir);

		Duration timeout = yaml.defaultTimeout() != null ? Duration.parse(yaml.defaultTimeout())
				: Duration.ofMinutes(5);

		return new DefaultBenchmark(yaml.name(), yaml.version() != null ? yaml.version() : "1.0", yaml.metadata(),
				items, yaml.jury(), timeout);
	}

	private List<BenchmarkTask> loadTasks(Path benchmarkDir) throws IOException {
		Path itemsDir = benchmarkDir.resolve("tasks");
		if (!Files.isDirectory(itemsDir)) {
			return List.of();
		}

		List<BenchmarkTask> items = new ArrayList<>();
		try (Stream<Path> dirs = Files.list(itemsDir)) {
			for (Path taskDir : dirs.filter(Files::isDirectory).toList()) {
				Path taskYamlFile = taskDir.resolve("task.yaml");
				if (Files.exists(taskYamlFile)) {
					items.add(loadTask(taskDir, taskYamlFile));
				}
			}
		}
		return items;
	}

	private BenchmarkTask loadTask(Path taskDir, Path taskYamlFile) throws IOException {
		TaskYaml yaml = yamlMapper.readValue(taskYamlFile.toFile(), TaskYaml.class);

		Path workspaceTemplate = taskDir.resolve("workspace");
		Path referenceDir = taskDir.resolve("reference");

		Duration timeout = yaml.timeout() != null ? Duration.parse(yaml.timeout()) : null;

		return new DefaultBenchmarkTask(yaml.id(), yaml.instruction(),
				Files.isDirectory(workspaceTemplate) ? workspaceTemplate : null,
				Files.isDirectory(referenceDir) ? referenceDir : null, yaml.metadata(), timeout, yaml.setup(),
				yaml.post());
	}

}
