package org.springaicommunity.bench.core.spec;

/** Git repo location + commit/tag. */
public record RepoSpec(String owner, // e.g. "your-org"
		String name, // e.g. "calculator-bench"
		String ref // branch, tag, or SHA
) {
}
