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

import org.springaicommunity.agents.judge.Judge;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Judgment;

/**
 * Core runner that executes benchmarks according to the genesis plan. Handles workspace
 * management, JBang agent invocation, judgment via Judge framework, and reporting.
 */
public class BenchRunner {

	private final Map<String, Judge> judges;

	public BenchRunner(Map<String, Judge> judges) {
		this.judges = judges;
	}

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

			// Execute judgment using Judge framework
			writeLog(logFile, "[JUDGE] Starting judgment via Judge framework");
			Judge judge = judges.get(runSpec.getCaseId());
			if (judge == null) {
				throw new IllegalStateException("No judge configured for case: " + runSpec.getCaseId());
			}

			JudgmentContext context = JudgmentContext.builder().workspace(workspace).build();
			Judgment judgment = judge.judge(context);

			Instant finishedAt = Instant.now();
			long durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();

			boolean success = judgment.pass();
			String status = success ? "success" : "failure";

			writeLog(logFile, "[RESULT] " + (success ? "SUCCESS" : "FAILURE") + ": " + judgment.reasoning());
			writeLog(logFile, "FINISHED: " + UTC_FORMATTER.format(finishedAt));

			// Generate reports
			writeLog(logFile, "[REPORTER] Generating reports");
			generateReports(runSpec, startedAt, finishedAt, durationMs, status, judgment, agentResult);

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

	private void generateReports(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs, String status,
			Judgment judgment, JBangResult agentResult) throws IOException {

		ReportGenerator reportGen = new ReportGenerator();
		reportGen.generateReports(runSpec, startedAt, finishedAt, durationMs, status, judgment, agentResult);
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
		Files.delete(path);
	}

	// Helper class for JBang result
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

}
