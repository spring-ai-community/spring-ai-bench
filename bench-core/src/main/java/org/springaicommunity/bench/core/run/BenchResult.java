package org.springaicommunity.bench.core.run;

public record BenchResult(String benchId, boolean passed, long durationMillis, String logPath // nullable
																								// for
																								// now
) {
}
