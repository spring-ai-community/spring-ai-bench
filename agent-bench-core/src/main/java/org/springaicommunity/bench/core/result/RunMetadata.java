package org.springaicommunity.bench.core.result;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Provenance metadata for a benchmark run. Written at run start and updated at
 * completion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunMetadata(String runId, String benchmarkName, String benchmarkVersion, String agentName,
		String agentCommand, int attempts, String startTime, String endTime, Double accuracy,
		Map<Integer, Double> passAtK, String commitHash) {

	/** Creates initial metadata at run start. */
	public static RunMetadata start(String runId, String benchmarkName, String benchmarkVersion, String agentCommand,
			int attempts) {
		String commitHash = resolveCommitHash();
		return new RunMetadata(runId, benchmarkName, benchmarkVersion, null, agentCommand, attempts,
				Instant.now().toString(), null, null, null, commitHash);
	}

	/** Creates completed metadata with final metrics. */
	public RunMetadata complete(double accuracy, Map<Integer, Double> passAtK) {
		return new RunMetadata(runId, benchmarkName, benchmarkVersion, agentName, agentCommand, attempts, startTime,
				Instant.now().toString(), accuracy, passAtK, commitHash);
	}

	private static String resolveCommitHash() {
		try {
			ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
			pb.redirectErrorStream(true);
			Process p = pb.start();
			if (p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0) {
				return new String(p.getInputStream().readAllBytes()).trim();
			}
		}
		catch (Exception ignored) {
		}
		return null;
	}

}
