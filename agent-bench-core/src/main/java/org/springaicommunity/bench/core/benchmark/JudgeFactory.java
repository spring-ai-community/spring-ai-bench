package org.springaicommunity.bench.core.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springaicommunity.judge.Judge;
import org.springaicommunity.judge.Judges;
import org.springaicommunity.judge.fs.FileContentJudge;
import org.springaicommunity.judge.fs.FileExistsJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.jury.CascadedJury;
import org.springaicommunity.judge.jury.MajorityVotingStrategy;
import org.springaicommunity.judge.jury.SimpleJury;
import org.springaicommunity.judge.jury.TierPolicy;
import org.springaicommunity.judge.jury.Verdict;
import org.springaicommunity.judge.result.Judgment;

/**
 * Materializes YAML jury configuration into {@link Judge} instances.
 */
public class JudgeFactory {

	/**
	 * Creates a Judge from a jury configuration map parsed from benchmark.yaml.
	 * @param juryConfig the jury configuration
	 * @return a Judge instance
	 */
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
		// Wrap Jury as Judge by delegating to vote() and extracting the aggregated
		// judgment
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
			Judge judge = switch (type) {
				case "file-exists" -> new FileExistsJudge((String) check.get("path"));
				case "file-content" -> {
					String matchMode = (String) check.getOrDefault("match", "EXACT");
					yield new FileContentJudge((String) check.get("path"), (String) check.get("expected"),
							FileContentJudge.MatchMode.valueOf(matchMode));
				}
				default -> throw new IllegalArgumentException("Unknown check type: " + type);
			};
			judges.add(judge);
		}
		return judges;
	}

}
