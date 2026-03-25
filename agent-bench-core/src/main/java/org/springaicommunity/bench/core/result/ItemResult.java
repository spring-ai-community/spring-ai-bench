package org.springaicommunity.bench.core.result;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Result of evaluating a single benchmark item.
 */
public record ItemResult(String itemId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
		Duration agentDuration, long tokens, Path workspace) {

	public ItemResult {
		scores = scores != null ? Map.copyOf(scores) : Map.of();
		if (failureMode == null) {
			failureMode = resolved ? FailureMode.NONE : FailureMode.AGENT_ERROR;
		}
	}

}
