package org.springaicommunity.bench.core.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springaicommunity.bench.core.agent.ExecAgentInvoker;
import org.springaicommunity.judge.Judge;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;

/**
 * Core runner that executes benchmarks. Handles workspace management, agent invocation,
 * judgment via Judge framework, and reporting.
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

			// Write instruction if the case spec provides one (optional —
			// purpose-built agents have their own baked-in instructions)
			String instruction = runSpec.getDescription();
			if (instruction != null && !instruction.isBlank()) {
				Files.writeString(workspace.resolve("INSTRUCTION.md"), instruction);
				writeLog(logFile, "[WORKSPACE] Wrote INSTRUCTION.md");
			}

			writeLog(logFile, "[AGENT] Starting agent invocation");
			AgentInvocationResult agentResult = invokeAgent(runSpec, workspace, logFile);

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

	private AgentInvocationResult invokeAgent(RunSpec runSpec, Path workspace, Path logFile) throws Exception {
		ExecAgentInvoker agent = loadAgentConfig(runSpec.getAgent().getAlias());
		String command = agent.command();
		long timeoutSec = agent.timeout().toSeconds();

		writeLog(logFile, "[AGENT] Running: " + command + " (in " + workspace + ")");

		ProcessResult result = new ProcessExecutor().command(command)
			.directory(workspace.toFile())
			.timeout(timeoutSec, TimeUnit.SECONDS)
			.readOutput(true)
			.redirectErrorStream(true)
			.execute();

		String output = result.outputUTF8();
		writeLog(logFile, "[AGENT] Output:\n" + output);
		writeLog(logFile, "[AGENT] Completed with exit code: " + result.getExitValue());

		return new AgentInvocationResult(result.getExitValue(), command, output);
	}

	private ExecAgentInvoker loadAgentConfig(String alias) throws IOException {
		Path agentYaml = Path.of("agents", alias + ".yaml");
		if (!Files.isRegularFile(agentYaml)) {
			agentYaml = agentYaml.toAbsolutePath();
		}
		if (!Files.isRegularFile(agentYaml)) {
			throw new IllegalStateException("Agent config not found: agents/" + alias + ".yaml");
		}
		return ExecAgentInvoker.fromYaml(agentYaml);
	}

	private void generateReports(RunSpec runSpec, Instant startedAt, Instant finishedAt, long durationMs, String status,
			Judgment judgment, AgentInvocationResult agentResult) throws IOException {

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
	static class AgentInvocationResult {

		private final int exitCode;

		private final String command;

		private final String output;

		public AgentInvocationResult(int exitCode, String command, String output) {
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
