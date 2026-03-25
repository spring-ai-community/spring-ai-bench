package org.springaicommunity.bench.core.result;

/**
 * Classifies why a benchmark item did not pass.
 */
public enum FailureMode {

	NONE, BUILD_FAILURE, TEST_FAILURE, TIMEOUT, AGENT_ERROR, GRADE_ERROR

}
