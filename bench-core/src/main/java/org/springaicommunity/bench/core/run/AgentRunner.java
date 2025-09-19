package org.springaicommunity.bench.core.run;

import org.springaicommunity.bench.core.spec.AgentSpec;
import java.nio.file.Path;
import java.time.Duration;

public interface AgentRunner {
    AgentResult run(Path workspace, AgentSpec spec, Duration timeout) throws Exception;
}
