package org.springaicommunity.bench.core.run;

import org.kohsuke.github.GitHub;
import org.springaicommunity.bench.core.repo.RepoWorkspaceManager;
import org.springaicommunity.bench.core.repo.Workspace;
import org.springaicommunity.bench.core.spec.BenchCase;

import java.time.Duration;
import java.time.Instant;

public class BenchHarness {

    private final RepoWorkspaceManager repoMgr;

    public BenchHarness(GitHub gh) {
        this.repoMgr = new RepoWorkspaceManager(gh);
    }

    /** Day-3: clone → dummy agent patch → gradle test. */
    public BenchResult run(BenchCase bc) throws Exception {
        try (Workspace ws = repoMgr.checkout(bc.repo(), Duration.ofMinutes(3))) {

            // 1.  run dummy agent
            AgentRunner runner   = new DummyPatchRunner();
            AgentResult agentRes = runner.run(ws.dir(), bc.agent(), Duration.ofMinutes(5));
            if (!agentRes.succeeded()) {
                return new BenchResult(bc.id(), false, agentRes.durationMillis(), null);
            }

            // 2.  verify tests
            SuccessVerifier verifier = new SuccessVerifier();
            boolean ok = verifier.verify(ws.dir(), bc.success(), Duration.ofMinutes(2));

            return new BenchResult(bc.id(), ok, agentRes.durationMillis(), null);
        }
    }
}
