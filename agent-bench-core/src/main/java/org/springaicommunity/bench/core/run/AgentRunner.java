package org.springaicommunity.bench.core.run;

import java.nio.file.Path;
import java.time.Duration;
import org.springaicommunity.bench.core.spec.AgentSpec;

public interface AgentRunner {

	AgentResult run(Path workspace, AgentSpec spec, Duration timeout) throws Exception;

}
