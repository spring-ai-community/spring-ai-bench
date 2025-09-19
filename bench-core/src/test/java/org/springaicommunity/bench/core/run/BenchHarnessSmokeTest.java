package org.springaicommunity.bench.core.run;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.springaicommunity.bench.core.io.BenchCaseLoader;
import org.springaicommunity.bench.core.spec.BenchCase;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BenchHarnessSmokeTest {

    private GitHub getGitHub() throws IOException {
        String githubToken = System.getenv("GITHUB_TOKEN");
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            return GitHub.connectUsingOAuth(githubToken);
        }
        return GitHub.connectAnonymously();
    }

    @Test
    void harnessClonesRepo() throws Exception {
        Path yaml = Path.of("src/test/resources/samples/calculator-sqrt-bug.yaml");
        BenchCase bench = BenchCaseLoader.load(yaml);
        BenchHarness harness = new BenchHarness(getGitHub());

        // The test passes if no exception is thrown.
        harness.run(bench);
    }
}
