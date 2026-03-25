package org.springaicommunity.bench.core.result;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Optional agent-reported journal. If an agent writes {@code journal.yaml} to the
 * workspace after execution, the bench parses it and attaches it to the trial result.
 * Agents that don't produce a journal still get graded — only efficiency metrics are
 * missing.
 *
 * <p>
 * Schema: {@code bench.journal.v1}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentJournal(String schema, List<Phase> phases, int totalTurns, long totalInputTokens,
		long totalOutputTokens, double totalCostUsd, long durationMs) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Phase(String name, int turns, long inputTokens, long outputTokens, double costUsd, long durationMs,
			Map<String, Integer> toolUses, String sessionId) {

		public Phase {
			toolUses = toolUses != null ? Map.copyOf(toolUses) : Map.of();
		}

	}

	public AgentJournal {
		phases = phases != null ? List.copyOf(phases) : List.of();
	}

	/** True if this journal has cost data for efficiency analysis. */
	public boolean hasCostData() {
		return totalCostUsd > 0.0 || phases.stream().anyMatch(p -> p.costUsd() > 0.0);
	}

}
