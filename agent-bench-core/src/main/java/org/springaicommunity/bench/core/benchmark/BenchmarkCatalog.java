package org.springaicommunity.bench.core.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

		Map<String, Object> juryConfig = resolveJuryPaths(yaml.jury(), benchmarkDir);

		return new DefaultBenchmark(yaml.name(), yaml.version() != null ? yaml.version() : "1.0", yaml.metadata(),
				items, juryConfig, timeout);
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

	/**
	 * Resolves relative file paths in jury check configs (e.g., prompt paths) to absolute
	 * paths using the benchmark directory as base.
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveJuryPaths(Map<String, Object> juryConfig, Path benchmarkDir) {
		if (juryConfig == null) {
			return null;
		}
		Map<String, Object> resolved = new HashMap<>(juryConfig);
		Object tiers = resolved.get("tiers");
		if (tiers instanceof List<?> tierList) {
			List<Object> resolvedTiers = new ArrayList<>();
			for (Object tier : tierList) {
				if (tier instanceof Map<?, ?> tierMap) {
					Map<String, Object> resolvedTier = new HashMap<>((Map<String, Object>) tierMap);
					Object checks = resolvedTier.get("checks");
					if (checks instanceof List<?> checkList) {
						List<Object> resolvedChecks = new ArrayList<>();
						for (Object check : checkList) {
							if (check instanceof Map<?, ?> checkMap) {
								Map<String, Object> resolvedCheck = new HashMap<>((Map<String, Object>) checkMap);
								resolvePathField(resolvedCheck, "prompt", benchmarkDir);
								resolvedChecks.add(resolvedCheck);
							}
							else {
								resolvedChecks.add(check);
							}
						}
						resolvedTier.put("checks", resolvedChecks);
					}
					resolvedTiers.add(resolvedTier);
				}
				else {
					resolvedTiers.add(tier);
				}
			}
			resolved.put("tiers", resolvedTiers);
		}
		return resolved;
	}

	private void resolvePathField(Map<String, Object> config, String field, Path baseDir) {
		Object value = config.get(field);
		if (value instanceof String path && !path.startsWith("/")) {
			config.put(field, baseDir.resolve(path).toAbsolutePath().toString());
		}
	}

	private BenchmarkTask loadTask(Path taskDir, Path taskYamlFile) throws IOException {
		TaskYaml yaml = yamlMapper.readValue(taskYamlFile.toFile(), TaskYaml.class);

		Path workspaceTemplate = taskDir.resolve("workspace");
		Path referenceDir = taskDir.resolve("reference");

		Duration timeout = yaml.timeout() != null ? Duration.parse(yaml.timeout()) : null;

		return new DefaultBenchmarkTask(yaml.id(), yaml.instruction(),
				Files.isDirectory(workspaceTemplate) ? workspaceTemplate : null,
				Files.isDirectory(referenceDir) ? referenceDir : null, yaml.metadata(), yaml.difficulty(), timeout,
				yaml.setup(), yaml.post());
	}

}
