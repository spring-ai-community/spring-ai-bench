package org.springaicommunity.bench.core.result;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Result of evaluating a single benchmark item (one trial). Captures the agent execution,
 * grading outcome, timestamps, and optional journal data.
 */
public record ItemResult(String itemId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
		Duration agentDuration, long tokens, Path workspace, Instant agentStartedAt, Instant agentEndedAt,
		Instant gradeStartedAt, Instant gradeEndedAt) {

	/** Compact constructor for backward compatibility (timestamps nullable). */
	public ItemResult {
		scores = scores != null ? Map.copyOf(scores) : Map.of();
		if (failureMode == null) {
			failureMode = resolved ? FailureMode.NONE : FailureMode.AGENT_ERROR;
		}
	}

	/** Convenience constructor without timestamps (legacy callers). */
	public ItemResult(String itemId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
			Duration agentDuration, long tokens, Path workspace) {
		this(itemId, resolved, scores, failureMode, agentDuration, tokens, workspace, null, null, null, null);
	}

}
