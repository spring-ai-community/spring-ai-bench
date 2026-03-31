package org.springaicommunity.bench.core.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class TrialResultTest {

	private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	void construction_withFullArguments() {
		Instant now = Instant.now();
		TrialResult result = new TrialResult("task-1", true, Map.of("key", "val"), FailureMode.NONE,
				Duration.ofSeconds(5), 1000, Path.of("/tmp/ws"), null, now, now.plusSeconds(5), now.plusSeconds(5),
				now.plusSeconds(6));

		assertThat(result.taskId()).isEqualTo("task-1");
		assertThat(result.resolved()).isTrue();
		assertThat(result.scores()).containsEntry("key", "val");
		assertThat(result.failureMode()).isEqualTo(FailureMode.NONE);
		assertThat(result.agentDuration()).isEqualTo(Duration.ofSeconds(5));
		assertThat(result.tokens()).isEqualTo(1000);
		assertThat(result.agentStartedAt()).isEqualTo(now);
	}

	@Test
	void construction_legacyWithoutTimestamps() {
		TrialResult result = new TrialResult("task-1", false, Map.of(), FailureMode.AGENT_TIMEOUT,
				Duration.ofSeconds(30), 0, Path.of("/tmp/ws"));

		assertThat(result.agentStartedAt()).isNull();
		assertThat(result.agentEndedAt()).isNull();
		assertThat(result.gradeStartedAt()).isNull();
		assertThat(result.gradeEndedAt()).isNull();
		assertThat(result.failureMode()).isEqualTo(FailureMode.AGENT_TIMEOUT);
	}

	@Test
	void scores_areCopiedDefensively() {
		Map<String, Object> mutable = new java.util.HashMap<>();
		mutable.put("a", 1);
		TrialResult result = new TrialResult("t", true, mutable, FailureMode.NONE, Duration.ZERO, 0, Path.of("/tmp"));
		assertThat(result.scores()).containsEntry("a", 1);
	}

	@Test
	void nullScores_becomeEmptyMap() {
		TrialResult result = new TrialResult("t", true, null, FailureMode.NONE, Duration.ZERO, 0, Path.of("/tmp"));
		assertThat(result.scores()).isEmpty();
	}

	@Test
	void nullFailureMode_defaultsBasedOnResolved() {
		TrialResult resolved = new TrialResult("t", true, Map.of(), null, Duration.ZERO, 0, Path.of("/tmp"));
		assertThat(resolved.failureMode()).isEqualTo(FailureMode.NONE);

		TrialResult failed = new TrialResult("t", false, Map.of(), null, Duration.ZERO, 0, Path.of("/tmp"));
		assertThat(failed.failureMode()).isEqualTo(FailureMode.AGENT_ERROR);
	}

	@Test
	void jsonRoundTrip() throws Exception {
		Instant now = Instant.parse("2025-01-01T00:00:00Z");
		TrialResult original = new TrialResult("task-1", true, Map.of("reasoning", "good"), FailureMode.NONE,
				Duration.ofSeconds(42), 5000, Path.of("/tmp/ws"), null, now, now.plusSeconds(42), now.plusSeconds(42),
				now.plusSeconds(43));

		String json = mapper.writeValueAsString(original);
		TrialResult deserialized = mapper.readValue(json, TrialResult.class);

		assertThat(deserialized.taskId()).isEqualTo("task-1");
		assertThat(deserialized.resolved()).isTrue();
		assertThat(deserialized.failureMode()).isEqualTo(FailureMode.NONE);
		assertThat(deserialized.tokens()).isEqualTo(5000);
		assertThat(deserialized.agentStartedAt()).isEqualTo(now);
	}

	@Test
	void trajectoryRef_presentWhenSet() {
		TrialResult result = new TrialResult("t", true, Map.of(), FailureMode.NONE, Duration.ZERO, 0, Path.of("/tmp"),
				"s3://bucket/trace.json", null, null, null, null);
		assertThat(result.trajectoryRef()).isEqualTo("s3://bucket/trace.json");
	}

	@Test
	void trajectoryRef_nullByDefault() {
		TrialResult result = new TrialResult("t", true, Map.of(), FailureMode.NONE, Duration.ZERO, 0, Path.of("/tmp"));
		assertThat(result.trajectoryRef()).isNull();
	}

	@Test
	void jsonDeserialization_ignoresUnknownFields() throws Exception {
		String json = """
				{"taskId":"t","resolved":true,"scores":{},"failureMode":"NONE",
				 "agentDuration":5.0,"tokens":0,"unknownField":"ignored"}
				""";
		TrialResult result = mapper.readValue(json, TrialResult.class);
		assertThat(result.taskId()).isEqualTo("t");
	}

}
