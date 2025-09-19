package org.springaicommunity.bench.core.run;

import java.nio.file.Path;

public record AgentResult(int exitCode, Path logFile, long durationMillis) {
    public boolean succeeded() { return exitCode == 0; }
}
