package org.springaicommunity.bench.core.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.core.result.BenchmarkResult;
import org.springaicommunity.bench.core.result.FailureMode;
import org.springaicommunity.bench.core.result.TrialResult;

class CompareCommandTest {

	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	void compare_printsSideBySideTable(@TempDir Path tempDir) throws Exception {
		// Set up two run directories with result.json
		Path run1 = tempDir.resolve("run1");
		Path run2 = tempDir.resolve("run2");
		Files.createDirectories(run1);
		Files.createDirectories(run2);

		TrialResult t1 = new TrialResult("task-a", true, Map.of(), FailureMode.NONE, Duration.ofSeconds(10), 0,
				Path.of("/tmp"));
		TrialResult t2 = new TrialResult("task-a", false, Map.of(), FailureMode.TEST_FAILURE, Duration.ofSeconds(20), 0,
				Path.of("/tmp"));

		BenchmarkResult r1 = BenchmarkResult.fromTrials("bench", "1.0", "run1", "claude-code", List.of(t1),
				Duration.ofSeconds(10), 0.05);
		BenchmarkResult r2 = BenchmarkResult.fromTrials("bench", "1.0", "run2", "gemini", List.of(t2),
				Duration.ofSeconds(20), 0.0);

		mapper.writerWithDefaultPrettyPrinter().writeValue(run1.resolve("result.json").toFile(), r1);
		mapper.writerWithDefaultPrettyPrinter().writeValue(run2.resolve("result.json").toFile(), r2);

		// Capture output
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream original = System.out;
		System.setOut(new PrintStream(baos));
		try {
			new CompareCommand().compare(List.of(run1, run2));
		}
		finally {
			System.setOut(original);
		}

		String output = baos.toString();
		assertThat(output).contains("claude-code");
		assertThat(output).contains("gemini");
		assertThat(output).contains("100.0%");
		assertThat(output).contains("0.0%");
	}

	@Test
	void compare_handlesEmptyList() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream original = System.out;
		System.setOut(new PrintStream(baos));
		try {
			new CompareCommand().compare(List.of());
		}
		finally {
			System.setOut(original);
		}
		assertThat(baos.toString()).contains("No results to compare");
	}

}
