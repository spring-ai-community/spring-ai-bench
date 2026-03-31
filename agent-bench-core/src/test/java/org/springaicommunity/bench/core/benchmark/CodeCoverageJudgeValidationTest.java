package org.springaicommunity.bench.core.benchmark;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.coverage.CoverageImprovementJudge;
import org.springaicommunity.judge.coverage.CoveragePreservationJudge;
import org.springaicommunity.judge.coverage.JaCoCoReportParser;
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
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CodeCoverageJudgeValidationTest {

	private static final Path WORKSPACE = Path.of(System.getProperty("user.home"),
			"projects/experiment-code-coverage-v2/results/code-coverage-v2/sessions",
			"20260314-003525/workspaces/hardened+skills+sae/spring-petclinic");

	@Test
	@Order(1)
	void t0_buildSuccess() {
		assumeTrue(Files.isDirectory(WORKSPACE), "Experiment workspace not found: " + WORKSPACE);

		// Include jacoco:report so the XML report is available for T1/T2
		BuildSuccessJudge judge = BuildSuccessJudge.maven("clean", "test", "jacoco:report",
				"-Dspring-javaformat.skip=true", "-Dcheckstyle.skip=true");
		JudgmentContext context = JudgmentContext.builder().workspace(WORKSPACE).build();

		Judgment judgment = judge.judge(context);

		assertThat(judgment.pass()).as("T0: build should succeed — agent-written tests compile and pass").isTrue();
	}

	@Test
	@Order(2)
	void t1_coveragePreservation() {
		assumeTrue(Files.isDirectory(WORKSPACE), "Experiment workspace not found: " + WORKSPACE);

		// Baseline: zero coverage (no tests before agent ran)
		JaCoCoReportParser.CoverageMetrics baseline = new JaCoCoReportParser.CoverageMetrics(0.0, 0.0, 0.0, 0, 0, 0, 0,
				0, 0, "No tests");

		CoveragePreservationJudge judge = new CoveragePreservationJudge();
		JudgmentContext context = JudgmentContext.builder()
			.workspace(WORKSPACE)
			.metadata("baselineCoverage", baseline)
			.build();

		Judgment judgment = judge.judge(context);

		assertThat(judgment.pass()).as("T1: coverage preserved — " + judgment.reasoning()).isTrue();
	}

	@Test
	@Order(3)
	void t2_coverageImprovement() {
		assumeTrue(Files.isDirectory(WORKSPACE), "Experiment workspace not found: " + WORKSPACE);

		JaCoCoReportParser.CoverageMetrics baseline = new JaCoCoReportParser.CoverageMetrics(0.0, 0.0, 0.0, 0, 0, 0, 0,
				0, 0, "No tests");

		CoverageImprovementJudge judge = new CoverageImprovementJudge(50.0);
		JudgmentContext context = JudgmentContext.builder()
			.workspace(WORKSPACE)
			.metadata("baselineCoverage", baseline)
			.build();

		Judgment judgment = judge.judge(context);

		assertThat(judgment.pass()).as("T2: coverage above 50%% — " + judgment.reasoning()).isTrue();
	}

}
