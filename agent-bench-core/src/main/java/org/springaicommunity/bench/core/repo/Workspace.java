package org.springaicommunity.bench.core.repo;

import java.nio.file.Path;

public record Workspace(Path dir) implements AutoCloseable {
	@Override
	public void close() {
		deleteQuietly();
	}

	public void deleteQuietly() {
		// recursive delete
		try (var stream = java.nio.file.Files.walk(dir)) {
			stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		}
		catch (Exception ignored) {
		}
	}
}
