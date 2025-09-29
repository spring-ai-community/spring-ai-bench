package org.springaicommunity.bench.core.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Core runner that executes benchmarks according to the genesis plan. Handles workspace
 * management, JBang agent invocation, verification, and reporting.
 */
public class BenchRunner {

	private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
		.withZone(ZoneOffset.UTC);

	public void execute(RunSpec runSpec) throws Exception {
		Instant startedAt = Instant.now();

		// Check for existing run unless force is specified
		checkExistingRun(runSpec);

		// Create workspace
		Path workspace = createWorkspace(runSpec);
		Path logFile = runSpec.getOutputDir().resolve("logs.txt");

		try {
			writeLog(logFile, "RUN_ID: " + runSpec.getRunId());
			writeLog(logFile, "STARTED: " + UTC_FORMATTER.format(startedAt));
			writeLog(logFile, "[WORKSPACE] Clearing existing workspace");

			// Invoke agent via JBang
			writeLog(logFile, "[AGENT] Starting agent invocation");
			JBangResult agentResult = invokeAgent(runSpec, workspace, logFile);

			// Verify results
			writeLog(logFile, "[VERIFIER] Starting verification");
			VerificationResult verificationResult = verifyResults(runSpec, workspace, logFile);

			Instant finishedAt = Instant.now();
			long durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();

			boolean success = agentResult.getExitCode() == 0 && verificationResult.isSuccess();
			String status = success ? "success" : "failure";

			writeLog(logFile, "[RESULT] " + (success ? "SUCCESS" : "FAILURE") + ": " + verificationResult.getReason());
			writeLog(logFile, "FINISHED: " + UTC_FORMATTER.format(finishedAt));

			// Generate reports
			writeLog(logFile, "[REPORTER] Generating reports");
			generateReports(runSpec, startedAt, finishedAt, durationMs, status, verificationResult, agentResult);

		}
		catch (Exception e) {
			Instant finishedAt = Instant.now();
			writeLog(logFile, "[ERROR] Agent execution failed: " + e.getMessage());
			writeLog(logFile, "FINISHED: " + UTC_FORMATTER.format(finishedAt));
			throw e;
		}
	}

	private void checkExistingRun(RunSpec runSpec) throws IOException {
		Path reportFile = runSpec.getOutputDir().resolve("report.json");
		if (Files.exists(reportFile) && !runSpec.isForce()) {
			throw new IllegalArgumentException("Run already exists at " + reportFile + " (use --force to overwrite)");
		}
	}

	private Path createWorkspace(RunSpec runSpec) throws IOException {
		Path outputDir = runSpec.getOutputDir();
		Path workspace = outputDir.resolve("workspace");

		// Create output directory structure
		Files.createDirectories(outputDir);

		// Clean and recreate workspace
		if (Files.exists(workspace)) {
			deleteRecursively(workspace);
		}
		Files.createDirectories(workspace);

		return workspace;
	}

	private JBangResult invokeAgent(RunSpec runSpec, Path workspace, Path logFile) throws Exception {
		// Build JBang command according to genesis plan
		List<String> command = new ArrayList<>();
		command.add("jbang");

		// Use absolute path for Phase 0-2
		command.add("/home/mark/community/spring-ai-agents/jbang/launcher.java");
		command.add(runSpec.getAgent().getAlias());

		// Add inputs as key=value pairs
		for (Map.Entry<String, Object> input : runSpec.getInputs().entrySet()) {
			command.add(input.getKey() + "=" + input.getValue());
		}

		// Record exact command for reproducibility
		String commandStr = String.join(" ", command);
		writeLog(logFile, "[AGENT] Invoking " + commandStr);

		// Execute with ProcessBuilder (safe for spaces/quotes)
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workspace.toFile());
		pb.redirectErrorStream(true);

		Process process = pb.start();

		// Capture output
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				writeLog(logFile, "[AGENT] " + line);
				output.append(line).append("\n");
			}
		}

		boolean finished = process.waitFor(5, TimeUnit.MINUTES);
		if (!finished) {
			process.destroyForcibly();
			throw new RuntimeException("Agent execution timed out after 5 minutes");
		}

		int exitCode = process.exitValue();
		writeLog(logFile, "[AGENT] Agent completed with exit code: " + exitCode);

		return new JBangResult(exitCode, commandStr, output.toString());
	}

	private VerificationResult verifyResults(RunSpec runSpec, Path workspace, Path logFile) {
		List<CheckResult> checkResults = new ArrayList<>();
		boolean allPassed = true;

		for (CheckSpec check : runSpec.getVerifier().getChecks()) {
			CheckResult result = performCheck(check, workspace);
			checkResults.add(result);

			String status = result.isPass() ? "PASS" : "FAIL";
			writeLog(logFile, "[VERIFIER] " + check.getType() + ":" + status
					+ (result.getDetails() != null ? " " + result.getDetails() : ""));

			if (!result.isPass()) {
				allPassed = false;
			}
		}

		String reason = allPassed ? "All checks passed"
				: "One or more checks failed: " + checkResults.stream()
					.filter(cr -> !cr.isPass())
					.map(cr -> cr.getName())
					.reduce((a, b) -> a + ", " + b)
					.orElse("unknown");

		return new VerificationResult(allPassed, reason, checkResults);
	}

	private CheckResult performCheck(CheckSpec check, Path workspace) {
		try {
			switch (check.getType()) {
				case "exists":
					return checkExists(check, workspace);
				case "equalsUtf8":
					return checkEqualsUtf8(check, workspace);
				default:
					return new CheckResult(check.getType(), false, "Unknown check type: " + check.getType());
			}
		}
		catch (Exception e) {
			return new CheckResult(check.getType(), false, "Error: " + e.getMessage());
		}
	}

	private CheckResult checkExists(CheckSpec check, Path workspace) {
		Path filePath = workspace.resolve(check.getPath());
		boolean exists = Files.exists(filePath);
		String details = exists ? "ok" : "not found";
		return new CheckResult("exists", exists, details);
	}

	private CheckResult checkEqualsUtf8(CheckSpec check, Path workspace) {
		try {
			Path filePath = workspace.resolve(check.getPath());
			if (!Files.exists(filePath)) {
				return new CheckResult("content", false, "file not found");
			}

			String actual = Files.readString(filePath);
			boolean matches = check.getExpected().equals(actual);
			String details = matches ? "ok" : "content mismatch";
			return new CheckResult("content", matches, details);
		}
		catch (IOException e) {
			return new CheckResult("content", false, "read error: " + e.getMessage());
		}
	}

	private void generateReports(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs, String status,
			VerificationResult verificationResult, JBangResult agentResult) throws IOException {

		ReportGenerator reportGen = new ReportGenerator();
		reportGen.generateReports(runSpec, startedAt, finishedAt, durationMs, status, verificationResult, agentResult);
	}

	private void writeLog(Path logFile, String message) {
		try {
			String timestamp = UTC_FORMATTER.format(Instant.now());
			String logLine = timestamp + " " + message + "\n";
			Files.write(logFile, logLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
		catch (IOException e) {
			System.err.println("Failed to write log: " + e.getMessage());
		}
	}

	private void deleteRecursively(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.list(path).forEach(child -> {
				try {
					deleteRecursively(child);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
		Files.delete(path);
	}

	// Helper classes for results
	static class JBangResult {

		private final int exitCode;

		private final String command;

		private final String output;

		public JBangResult(int exitCode, String command, String output) {
			this.exitCode = exitCode;
			this.command = command;
			this.output = output;
		}

		public int getExitCode() {
			return exitCode;
		}

		public String getCommand() {
			return command;
		}

		public String getOutput() {
			return output;
		}

	}

	static class VerificationResult {

		private final boolean success;

		private final String reason;

		private final List<CheckResult> checkResults;

		public VerificationResult(boolean success, String reason, List<CheckResult> checkResults) {
			this.success = success;
			this.reason = reason;
			this.checkResults = checkResults;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getReason() {
			return reason;
		}

		public List<CheckResult> getCheckResults() {
			return checkResults;
		}

	}

	static class CheckResult {

		private final String name;

		private final boolean pass;

		private final String details;

		public CheckResult(String name, boolean pass, String details) {
			this.name = name;
			this.pass = pass;
			this.details = details;
		}

		public String getName() {
			return name;
		}

		public boolean isPass() {
			return pass;
		}

		public String getDetails() {
			return details;
		}

	}

}