package org.springaicommunity.bench.core.result;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Result of evaluating a single benchmark task (one trial). Captures the agent execution,
 * grading outcome, timestamps, and optional journal data.
 */
public record TrialResult(String taskId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
		Duration agentDuration, long tokens, Path workspace, Instant agentStartedAt, Instant agentEndedAt,
		Instant gradeStartedAt, Instant gradeEndedAt) {

	/** Compact constructor for backward compatibility (timestamps nullable). */
	public TrialResult {
		scores = scores != null ? Map.copyOf(scores) : Map.of();
		if (failureMode == null) {
			failureMode = resolved ? FailureMode.NONE : FailureMode.AGENT_ERROR;
		}
	}

	/** Convenience constructor without timestamps (legacy callers). */
	public TrialResult(String taskId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
			Duration agentDuration, long tokens, Path workspace) {
		this(taskId, resolved, scores, failureMode, agentDuration, tokens, workspace, null, null, null, null);
	}

}
