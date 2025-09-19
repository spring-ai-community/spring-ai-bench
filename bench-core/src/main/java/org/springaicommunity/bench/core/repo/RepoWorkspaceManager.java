package org.springaicommunity.bench.core.repo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springaicommunity.bench.core.spec.RepoSpec;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

public final class RepoWorkspaceManager {

	private final GitHub github;

	public RepoWorkspaceManager(GitHub github) {
		this.github = github;
	}

	/** Clone repo@ref into a temp dir; throws IOException on failure. */
	public Workspace checkout(RepoSpec spec, Duration cloneTimeout) throws IOException, InterruptedException {
		GHRepository gh = github.getRepository(spec.owner() + "/" + spec.name());
		String cloneUrl = gh.getHttpTransportUrl();

		Path workspace = Files.createTempDirectory("sai-bench-workspace-");
		Path repoDir = workspace.resolve("repo");

		if (looksLikeSha(spec.ref())) {
			// clone, then checkout SHA - clone into a subdirectory first
			run(new String[] { "git", "clone", cloneUrl, repoDir.toString() }, null, cloneTimeout, "git clone");
			run(new String[] { "git", "-C", repoDir.toString(), "checkout", spec.ref() }, null, cloneTimeout,
					"git checkout");
		}
		else {
			run(new String[] { "git", "clone", "--depth", "1", "--branch", spec.ref(), cloneUrl, repoDir.toString() },
					null, cloneTimeout, "git clone");
		}

		return new Workspace(repoDir);
	}

	/* ------------------------------------------------------------------ */
	private static void run(String[] command, Path workingDir, Duration timeout, String step)
			throws IOException, InterruptedException {
		try {
			ProcessExecutor executor = new ProcessExecutor().command(command)
				.timeout(timeout.toSeconds(), TimeUnit.SECONDS)
				.exitValues(0)
				.readOutput(true);

			if (workingDir != null) {
				executor = executor.directory(workingDir.toFile());
			}

			executor.execute();
		}
		catch (InvalidExitValueException e) {
			throw new IOException(
					step + " failed (exit code: " + e.getExitValue() + ") output: " + e.getResult().outputUTF8(), e);
		}
		catch (java.util.concurrent.TimeoutException e) {
			throw new IOException("Timeout during: " + step, e);
		}
	}

	private static boolean looksLikeSha(String s) {
		return s.matches("[0-9a-fA-F]{7,40}");
	}

}
