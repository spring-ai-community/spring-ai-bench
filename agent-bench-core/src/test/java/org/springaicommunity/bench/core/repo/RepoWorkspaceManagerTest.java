package org.springaicommunity.bench.core.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.springaicommunity.bench.core.spec.RepoSpec;

class RepoWorkspaceManagerTest {

	private GitHub getGitHub() throws IOException {
		String githubToken = System.getenv("GITHUB_TOKEN");
		if (githubToken != null && !githubToken.trim().isEmpty()) {
			return GitHub.connectUsingOAuth(githubToken);
		}
		return GitHub.connectAnonymously();
	}

	@Test
	void cloneAndDelete() throws Exception {
		RepoWorkspaceManager mgr = new RepoWorkspaceManager(getGitHub());
		RepoSpec spec = new RepoSpec("rd-1-2022", "simple-calculator", "93da3b1847ed67f3bc7d8a84e1e6afd737f1a555");
		Workspace ws = mgr.checkout(spec, Duration.ofMinutes(2));

		assertTrue(Files.exists(ws.dir())); // clone successful
		Path p = ws.dir();
		ws.deleteQuietly();
		assertFalse(Files.exists(p)); // cleanup verified
	}

}
