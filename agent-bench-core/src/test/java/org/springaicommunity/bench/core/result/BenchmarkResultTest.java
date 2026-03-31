package org.springaicommunity.bench.core.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BenchmarkResultTest {

	private static TrialResult trial(String taskId, boolean resolved) {
		return new TrialResult(taskId, resolved, Map.of(), resolved ? FailureMode.NONE : FailureMode.TEST_FAILURE,
				Duration.ofSeconds(10), 0, Path.of("/tmp"));
	}

	@Test
	void fromTrials_computesAccuracy() {
		List<TrialResult> trials = List.of(trial("a", true), trial("b", false), trial("c", true));
		BenchmarkResult result = BenchmarkResult.fromTrials("bench", "1.0", "run-1", "agent", trials,
				Duration.ofMinutes(1), 0.0);

		assertThat(result.accuracy()).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
	}

	@Test
	void fromTrials_emptyTrials_zeroAccuracy() {
		BenchmarkResult result = BenchmarkResult.fromTrials("bench", "1.0", "run-1", "agent", List.of(), Duration.ZERO,
				0.0);
		assertThat(result.accuracy()).isEqualTo(0.0);
	}

	@Test
	void fromTrials_allResolved_perfectAccuracy() {
		List<TrialResult> trials = List.of(trial("a", true), trial("b", true));
		BenchmarkResult result = BenchmarkResult.fromTrials("bench", "1.0", "run-1", "agent", trials, Duration.ZERO,
				0.0);
		assertThat(result.accuracy()).isEqualTo(1.0);
	}

	@Test
	void passAtK_singleAttempt_equalsAccuracy() {
		List<TrialResult> trials = List.of(trial("a", true), trial("b", false));
		Map<Integer, Double> passAtK = BenchmarkResult.computePassAtK(trials);
		assertThat(passAtK).containsKey(1);
		assertThat(passAtK.get(1)).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001));
	}

	@Test
	void passAtK_multipleAttempts() {
		// 2 tasks, 3 attempts each. Task a: 2/3 pass, Task b: 1/3 pass
		List<TrialResult> trials = List.of(trial("a", true), trial("a", true), trial("a", false), trial("b", true),
				trial("b", false), trial("b", false));

		Map<Integer, Double> passAtK = BenchmarkResult.computePassAtK(trials);
		assertThat(passAtK).containsKeys(1, 2);
		// pass@1 should be > 0 (both tasks have at least one success)
		assertThat(passAtK.get(1)).isGreaterThan(0.0);
		// pass@2 should be >= pass@1 (more attempts = higher chance)
		assertThat(passAtK.get(2)).isGreaterThanOrEqualTo(passAtK.get(1));
	}

	@Test
	void passAtK_allFail_returnsZero() {
		List<TrialResult> trials = List.of(trial("a", false), trial("a", false));
		Map<Integer, Double> passAtK = BenchmarkResult.computePassAtK(trials);
		assertThat(passAtK.get(1)).isEqualTo(0.0);
	}

	@Test
	void passAtK_emptyTrials_returnsEmpty() {
		Map<Integer, Double> passAtK = BenchmarkResult.computePassAtK(List.of());
		assertThat(passAtK).isEmpty();
	}

	@Test
	void tierScores_computesMeansOfNumericScores() {
		TrialResult t1 = new TrialResult("a", true, Map.of("costUsd", 0.10, "turns", 5), FailureMode.NONE,
				Duration.ofSeconds(10), 0, Path.of("/tmp"));
		TrialResult t2 = new TrialResult("b", true, Map.of("costUsd", 0.20, "turns", 3), FailureMode.NONE,
				Duration.ofSeconds(10), 0, Path.of("/tmp"));

		Map<String, Double> tierScores = BenchmarkResult.computeTierScores(List.of(t1, t2));
		assertThat(tierScores.get("costUsd")).isCloseTo(0.15, org.assertj.core.data.Offset.offset(0.001));
		assertThat(tierScores.get("turns")).isCloseTo(4.0, org.assertj.core.data.Offset.offset(0.001));
	}

	@Test
	void tierScores_emptyWhenNoNumericScores() {
		Map<String, Double> tierScores = BenchmarkResult
			.computeTierScores(List.of(trial("a", true), trial("b", false)));
		assertThat(tierScores).isEmpty();
	}

	@Test
	void tierScores_includedInAggregateScores() {
		TrialResult t1 = new TrialResult("a", true, Map.of("costUsd", 0.10), FailureMode.NONE, Duration.ofSeconds(10),
				0, Path.of("/tmp"));
		BenchmarkResult result = BenchmarkResult.fromTrials("bench", "1.0", "run-1", "agent", List.of(t1),
				Duration.ZERO, 0.0);
		assertThat(result.aggregateScores()).containsKey("tierScores");
	}

	@Test
	void trialsAreImmutable() {
		List<TrialResult> trials = new java.util.ArrayList<>(List.of(trial("a", true)));
		BenchmarkResult result = BenchmarkResult.fromTrials("bench", "1.0", "run-1", "agent", trials, Duration.ZERO,
				0.0);
		assertThat(result.trials()).hasSize(1);
		// Mutating the original list should not affect the result
		trials.add(trial("b", false));
		assertThat(result.trials()).hasSize(1);
	}

}
