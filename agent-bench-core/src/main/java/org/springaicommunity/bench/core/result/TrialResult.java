package org.springaicommunity.bench.core.result;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Result of evaluating a single benchmark task (one trial). Captures the agent execution,
 * grading outcome, timestamps, and optional journal data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TrialResult(String taskId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
		Duration agentDuration, long tokens, Path workspace, String trajectoryRef, Instant agentStartedAt,
		Instant agentEndedAt, Instant gradeStartedAt, Instant gradeEndedAt) {

	/** Compact constructor for backward compatibility (timestamps nullable). */
	public TrialResult {
		scores = scores != null ? Map.copyOf(scores) : Map.of();
		if (failureMode == null) {
			failureMode = resolved ? FailureMode.NONE : FailureMode.AGENT_ERROR;
		}
	}

	/** Convenience constructor without timestamps and trajectory (legacy callers). */
	public TrialResult(String taskId, boolean resolved, Map<String, Object> scores, FailureMode failureMode,
			Duration agentDuration, long tokens, Path workspace) {
		this(taskId, resolved, scores, failureMode, agentDuration, tokens, workspace, null, null, null, null, null);
	}

}
