package org.springaicommunity.bench.core.result;

/**
 * Classifies why a benchmark item did not pass.
 */
public enum FailureMode {

	/** No failure — item resolved successfully. */
	NONE,

	/** Agent process timed out. */
	AGENT_TIMEOUT,

	/** Agent process exited with non-zero code (not timeout). */
	AGENT_ERROR,

	/** Agent hit LLM context length limit. */
	CONTEXT_LENGTH_EXCEEDED,

	/** Setup script failed before agent ran. */
	SETUP_ERROR,

	/** Judge/grading itself failed (e.g., JaCoCo parse error). */
	GRADE_ERROR,

	/** Maven build failed during grading. */
	BUILD_FAILURE,

	/** Tests failed during grading. */
	TEST_FAILURE,

	/** Catch-all for unclassified failures. */
	UNKNOWN

}
