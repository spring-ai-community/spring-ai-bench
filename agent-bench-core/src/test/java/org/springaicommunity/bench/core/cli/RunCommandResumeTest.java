package org.springaicommunity.bench.core.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.core.result.RunMetadata;

class RunCommandResumeTest {

	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	void resume_throwsForMissingRunDir() {
		RunCommand cmd = new RunCommand(Path.of("benchmarks"));
		assertThatThrownBy(() -> cmd.resume("nonexistent-run-id")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Run directory not found");
	}

	@Test
	void resume_readsExistingMetadata(@TempDir Path tempDir) throws Exception {
		// Set up a fake run directory with metadata
		String runId = "test-resume-001";
		Path runDir = tempDir.resolve("runs").resolve(runId);
		Files.createDirectories(runDir.resolve("tasks"));

		RunMetadata metadata = RunMetadata.start(runId, "hello-world", "1.0", "echo hello", 1);
		mapper.writerWithDefaultPrettyPrinter().writeValue(runDir.resolve("run-metadata.json").toFile(), metadata);

		// Verify metadata is readable
		RunMetadata loaded = mapper.readValue(runDir.resolve("run-metadata.json").toFile(), RunMetadata.class);
		assertThat(loaded.runId()).isEqualTo(runId);
		assertThat(loaded.benchmarkName()).isEqualTo("hello-world");
		assertThat(loaded.attempts()).isEqualTo(1);
	}

}
