package org.springaicommunity.bench.agents.judge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Check;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;
import org.springaicommunity.judge.score.NumericalScore;

/**
 * LLM-based judge that evaluates practice adherence of a test suite against a rubric
 * prompt. Copied from experiment-code-coverage-v2 (frozen experiment). Produces practice
 * adherence scores — a separate dimension from functional correctness (T0–T2
 * deterministic judges).
 */
public class TestQualityJudge implements Judge {

	private static final Logger logger = LoggerFactory.getLogger(TestQualityJudge.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");

	private final Function<Path, AgentClient> agentClientFactory;

	private final Path judgePromptPath;

	private final double passThreshold;

	public TestQualityJudge(Function<Path, AgentClient> agentClientFactory, Path judgePromptPath,
			double passThreshold) {
		this.agentClientFactory = agentClientFactory;
		this.judgePromptPath = judgePromptPath;
		this.passThreshold = passThreshold;
	}

	public TestQualityJudge(Function<Path, AgentClient> agentClientFactory, Path judgePromptPath) {
		this(agentClientFactory, judgePromptPath, 0.5);
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		Path workspace = context.workspace();

		if (!hasTestFiles(workspace)) {
			logger.info("No test files found in {}", workspace);
			return Judgment.builder()
				.score(NumericalScore.normalized(0.0))
				.status(JudgmentStatus.FAIL)
				.reasoning("No test classes found in src/test/")
				.build();
		}

		String judgePrompt;
		try {
			judgePrompt = Files.readString(judgePromptPath);
		}
		catch (IOException ex) {
			logger.error("Failed to read judge prompt: {}", judgePromptPath, ex);
			return Judgment.error("Failed to read judge prompt: " + ex.getMessage(), ex);
		}

		String agentOutput;
		try {
			AgentClient client = agentClientFactory.apply(workspace);
			AgentClientResponse response = client.goal(judgePrompt).workingDirectory(workspace).run();
			agentOutput = response.getResult();
		}
		catch (Exception ex) {
			logger.error("Judge agent execution failed", ex);
			return Judgment.error("Judge agent execution failed: " + ex.getMessage(), ex);
		}

		return parseJudgment(agentOutput);
	}

	Judgment parseJudgment(String agentOutput) {
		Matcher matcher = JSON_BLOCK.matcher(agentOutput);
		if (!matcher.find()) {
			return Judgment.error("No JSON found in judge output", null);
		}

		String json = matcher.group();
		try {
			JsonNode root = MAPPER.readTree(json);
			JsonNode criteriaNode = root.get("criteria");
			if (criteriaNode == null || !criteriaNode.isArray()) {
				return Judgment.error("Judge output missing 'criteria' array", null);
			}

			List<Check> checks = new ArrayList<>();
			double scoreSum = 0.0;
			int count = 0;

			for (JsonNode criterion : criteriaNode) {
				String name = criterion.has("name") ? criterion.get("name").asText() : "criterion_" + count;
				double rawScore = criterion.has("score") ? criterion.get("score").asDouble() : 0.0;
				String evidence = criterion.has("evidence") ? criterion.get("evidence").asText() : "";
				double score = Math.max(0.0, Math.min(1.0, rawScore));
				checks.add(new Check(name, score >= passThreshold, String.format("%.2f — %s", score, evidence)));
				scoreSum += score;
				count++;
			}

			if (count == 0) {
				return Judgment.error("Judge output has empty criteria array", null);
			}

			double averageScore = scoreSum / count;
			JudgmentStatus status = averageScore >= passThreshold ? JudgmentStatus.PASS : JudgmentStatus.FAIL;
			String bootVersion = root.has("boot_version") ? root.get("boot_version").asText() : "unknown";

			return Judgment.builder()
				.score(NumericalScore.normalized(averageScore))
				.status(status)
				.reasoning(String.format("Practice adherence: %.2f avg across %d criteria (Boot %s)", averageScore,
						count, bootVersion))
				.checks(checks)
				.metadata("boot_version", bootVersion)
				.metadata("criteria_count", count)
				.metadata("raw_json", json)
				.build();
		}
		catch (Exception ex) {
			return Judgment.error("Failed to parse judge JSON: " + ex.getMessage(), ex);
		}
	}

	private boolean hasTestFiles(Path workspace) {
		Path testDir = workspace.resolve("src/test");
		if (!Files.isDirectory(testDir)) {
			return false;
		}
		try (Stream<Path> walk = Files.walk(testDir)) {
			return walk.anyMatch(p -> p.toString().endsWith(".java"));
		}
		catch (IOException ex) {
			logger.warn("Failed to scan test directory: {}", testDir, ex);
			return false;
		}
	}

	public static Function<Path, AgentClient> defaultAgentClientFactory(String model, Duration timeout) {
		return workspace -> {
			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model(model)
				.allowedTools(List.of("Read", "Glob", "Grep"))
				.yolo(false)
				.timeout(timeout)
				.build();

			AgentModel agentModel = ClaudeAgentModel.builder()
				.workingDirectory(workspace)
				.defaultOptions(options)
				.build();

			return AgentClient.create(agentModel);
		};
	}

}
