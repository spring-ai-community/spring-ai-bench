package org.springaicommunity.bench.core.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springaicommunity.bench.core.benchmark.Benchmark;
import org.springaicommunity.bench.core.benchmark.BenchmarkCatalog;
import org.springaicommunity.bench.core.benchmark.BenchmarkItem;

/**
 * Handles the {@code list} and {@code items} CLI commands.
 */
public class ListCommand {

	private final BenchmarkCatalog catalog;

	public ListCommand(Path benchmarksDir) {
		this.catalog = new BenchmarkCatalog(benchmarksDir);
	}

	public ListCommand() {
		this(Paths.get("benchmarks"));
	}

	/**
	 * Lists all discovered benchmarks.
	 */
	public void listBenchmarks() throws IOException {
		List<Benchmark> benchmarks = catalog.discover();
		if (benchmarks.isEmpty()) {
			System.out.println("No benchmarks found in benchmarks/ directory.");
			return;
		}

		System.out.println("Available benchmarks:");
		System.out.println();
		for (Benchmark b : benchmarks) {
			System.out.printf("  %-30s v%-8s (%d items)%n", b.name(), b.version(), b.items().size());
		}
	}

	/**
	 * Lists items within a specific benchmark.
	 */
	public void listItems(String benchmarkName) throws IOException {
		List<Benchmark> benchmarks = catalog.discover();
		Benchmark target = benchmarks.stream()
			.filter(b -> b.name().equals(benchmarkName))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Benchmark not found: " + benchmarkName));

		System.out.printf("Items in benchmark '%s' (v%s):%n", target.name(), target.version());
		System.out.println();
		for (BenchmarkItem item : target.items()) {
			String timeout = item.timeout() != null ? item.timeout().toString() : target.defaultTimeout().toString();
			System.out.printf("  %-30s timeout=%s%n", item.id(), timeout);
			if (item.instruction() != null) {
				String instr = item.instruction();
				if (instr.length() > 80) {
					instr = instr.substring(0, 77) + "...";
				}
				System.out.printf("    %s%n", instr);
			}
		}
	}

}
