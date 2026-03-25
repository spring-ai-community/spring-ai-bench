package org.springaicommunity.bench.core.benchmark;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.coverage.CoverageImprovementJudge;
import org.springaicommunity.judge.coverage.CoveragePreservationJudge;
import org.springaicommunity.judge.exec.BuildSuccessJudge;
import org.springaicommunity.judge.result.Judgment;

/**
 * Validates T0-T2 judges against a preserved workspace from the code-coverage-v2
 * experiment (best variant: hardened+skills+preanalysis, session 20260314-003525). The
 * workspace has 11 agent-written test files and a JaCoCo report showing 91.5% coverage.
 *
 * <p>
 * This test requires the experiment results to be present on the local filesystem. It is
 * skipped if the workspace is not found.
 */
class CodeCoverageJudgeValidationTest {

	private static final Path WORKSPACE = Path.of(System.getProperty("user.home"),
			"projects/experiment-code-coverage-v2/results/code-coverage-v2/sessions",
			"20260314-003525/workspaces/hardened+skills+sae/spring-petclinic");

	@Test
	void t0_buildSuccess() {
		assumeTrue(Files.isDirectory(WORKSPACE), "Experiment workspace not found: " + WORKSPACE);

		BuildSuccessJudge judge = BuildSuccessJudge.maven("clean", "test", "-Dspring-javaformat.skip=true",
				"-Dcheckstyle.skip=true");
		JudgmentContext context = JudgmentContext.builder().workspace(WORKSPACE).build();

		Judgment judgment = judge.judge(context);

		assertThat(judgment.pass()).as("T0: build should succeed — agent-written tests compile and pass").isTrue();
	}

	@Test
	void t1_coveragePreservation() {
		assumeTrue(Files.isDirectory(WORKSPACE), "Experiment workspace not found: " + WORKSPACE);

		CoveragePreservationJudge judge = new CoveragePreservationJudge();
		JudgmentContext context = JudgmentContext.builder().workspace(WORKSPACE).build();

		Judgment judgment = judge.judge(context);

		// Baseline was 0% — any coverage preserves it
		assertThat(judgment.pass()).as("T1: coverage should be preserved (baseline was 0%)").isTrue();
	}

	@Test
	void t2_coverageImprovement() {
		assumeTrue(Files.isDirectory(WORKSPACE), "Experiment workspace not found: " + WORKSPACE);

		CoverageImprovementJudge judge = new CoverageImprovementJudge(50.0);
		JudgmentContext context = JudgmentContext.builder().workspace(WORKSPACE).build();

		Judgment judgment = judge.judge(context);

		// Experiment recorded 91.5% coverage — well above 50% threshold
		assertThat(judgment.pass()).as("T2: coverage should exceed 50% threshold (experiment: 91.5%)").isTrue();
	}

}
