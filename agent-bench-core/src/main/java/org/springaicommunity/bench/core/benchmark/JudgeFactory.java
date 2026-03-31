package org.springaicommunity.bench.core.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.Judges;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.coverage.CoverageImprovementJudge;
import org.springaicommunity.judge.coverage.CoveragePreservationJudge;
import org.springaicommunity.judge.exec.BuildSuccessJudge;
import org.springaicommunity.judge.fs.FileContentJudge;
import org.springaicommunity.judge.fs.FileExistsJudge;
import org.springaicommunity.judge.jury.CascadedJury;
import org.springaicommunity.judge.jury.MajorityVotingStrategy;
import org.springaicommunity.judge.jury.SimpleJury;
import org.springaicommunity.judge.jury.TierPolicy;
import org.springaicommunity.judge.jury.Verdict;

/**
 * Materializes YAML jury configuration into {@link Judge} instances. Supports built-in
 * check types (file-exists, file-content, maven-build, coverage-preservation,
 * coverage-improvement) and custom types registered via {@link #register}.
 */
public class JudgeFactory {

	private final Map<String, Function<Map<String, Object>, Judge>> registry = new HashMap<>();

	public JudgeFactory() {
		// Built-in types
		registry.put("file-exists", config -> new FileExistsJudge((String) config.get("path")));
		registry.put("file-content", config -> {
			String matchMode = (String) config.getOrDefault("match", "EXACT");
			return new FileContentJudge((String) config.get("path"), (String) config.get("expected"),
					FileContentJudge.MatchMode.valueOf(matchMode));
		});
		registry.put("maven-build", config -> {
			@SuppressWarnings("unchecked")
			List<String> goals = (List<String>) config.getOrDefault("goals", List.of("clean", "test"));
			return BuildSuccessJudge.maven(goals.toArray(new String[0]));
		});
		registry.put("coverage-preservation", config -> new CoveragePreservationJudge());
		registry.put("coverage-improvement", config -> {
			double threshold = config.containsKey("min") ? ((Number) config.get("min")).doubleValue() : 50.0;
			return new CoverageImprovementJudge(threshold);
		});
	}

	/**
	 * Registers a custom judge type. Use this to add domain-specific judges (e.g.,
	 * test-quality-llm) from outside agent-bench-core.
	 */
	public void register(String type, Function<Map<String, Object>, Judge> factory) {
		registry.put(type, factory);
	}

	@SuppressWarnings("unchecked")
	public Judge createFromConfig(Map<String, Object> juryConfig) {
		if (juryConfig == null || juryConfig.isEmpty()) {
			throw new IllegalArgumentException("Jury configuration is required");
		}

		Object tiers = juryConfig.get("tiers");
		if (tiers instanceof List) {
			return createCascade((List<Map<String, Object>>) tiers);
		}

		Object checks = juryConfig.get("checks");
		if (checks instanceof List) {
			return createFromChecks((List<Map<String, Object>>) checks);
		}

		throw new IllegalArgumentException("Jury config must contain 'tiers' or 'checks'");
	}

	@SuppressWarnings("unchecked")
	private Judge createCascade(List<Map<String, Object>> tiers) {
		CascadedJury.Builder builder = CascadedJury.builder();

		for (int i = 0; i < tiers.size(); i++) {
			Map<String, Object> tierMap = tiers.get(i);
			String name = (String) tierMap.getOrDefault("name", "tier-" + (i + 1));
			String policyStr = (String) tierMap.getOrDefault("policy",
					i == tiers.size() - 1 ? "FINAL_TIER" : "REJECT_ON_ANY_FAIL");
			TierPolicy policy = TierPolicy.valueOf(policyStr);

			List<Map<String, Object>> checks = (List<Map<String, Object>>) tierMap.get("checks");
			List<Judge> judges = createJudgesFromChecks(checks);

			SimpleJury.Builder juryBuilder = SimpleJury.builder().votingStrategy(new MajorityVotingStrategy());
			judges.forEach(juryBuilder::judge);
			SimpleJury tierJury = juryBuilder.build();
			builder.tier(name, tierJury, policy);
		}

		CascadedJury jury = builder.build();
		return (JudgmentContext ctx) -> {
			Verdict verdict = jury.vote(ctx);
			return verdict.aggregated();
		};
	}

	private Judge createFromChecks(List<Map<String, Object>> checks) {
		List<Judge> judges = createJudgesFromChecks(checks);
		return Judges.allOf(judges.toArray(new Judge[0]));
	}

	private List<Judge> createJudgesFromChecks(List<Map<String, Object>> checks) {
		List<Judge> judges = new ArrayList<>();
		if (checks == null) {
			return judges;
		}

		for (Map<String, Object> check : checks) {
			String type = (String) check.get("type");
			Function<Map<String, Object>, Judge> factory = registry.get(type);
			if (factory == null) {
				throw new IllegalArgumentException("Unknown check type: " + type + ". Available: " + registry.keySet());
			}
			judges.add(factory.apply(check));
		}
		return judges;
	}

}
