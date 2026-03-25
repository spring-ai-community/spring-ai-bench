package org.springaicommunity.bench.core.spec;

public record BenchCase(String id, String category, // "coding", "project-mgmt",
													// "version-upgrade"
		String version, RepoSpec repo, AgentSpec agent, SuccessSpec success, Long timeoutSec) {
}
