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

		List<BenchmarkItem> items = loadItems(benchmarkDir);

		Duration timeout = yaml.defaultTimeout() != null ? Duration.parse(yaml.defaultTimeout())
				: Duration.ofMinutes(5);

		return new DefaultBenchmark(yaml.name(), yaml.version() != null ? yaml.version() : "1.0", yaml.metadata(),
				items, yaml.jury(), timeout);
	}

	private List<BenchmarkItem> loadItems(Path benchmarkDir) throws IOException {
		Path itemsDir = benchmarkDir.resolve("items");
		if (!Files.isDirectory(itemsDir)) {
			return List.of();
		}

		List<BenchmarkItem> items = new ArrayList<>();
		try (Stream<Path> dirs = Files.list(itemsDir)) {
			for (Path itemDir : dirs.filter(Files::isDirectory).toList()) {
				Path itemYamlFile = itemDir.resolve("item.yaml");
				if (Files.exists(itemYamlFile)) {
					items.add(loadItem(itemDir, itemYamlFile));
				}
			}
		}
		return items;
	}

	private BenchmarkItem loadItem(Path itemDir, Path itemYamlFile) throws IOException {
		ItemYaml yaml = yamlMapper.readValue(itemYamlFile.toFile(), ItemYaml.class);

		Path workspaceTemplate = itemDir.resolve("workspace");
		Path referenceDir = itemDir.resolve("reference");

		Duration timeout = yaml.timeout() != null ? Duration.parse(yaml.timeout()) : null;

		return new DefaultBenchmarkItem(yaml.id(), yaml.instruction(),
				Files.isDirectory(workspaceTemplate) ? workspaceTemplate : null,
				Files.isDirectory(referenceDir) ? referenceDir : null, yaml.metadata(), timeout, yaml.setup(),
				yaml.post());
	}

}
