/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.bench.agents.support;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

/** Utility class for CLI operations using zt-exec. */
public class CliUtils {

	/**
	 * Get CLI version using resolved path and zt-exec
	 * @param cliPath Resolved CLI path from discovery
	 * @param timeout Timeout duration
	 * @return Version string or "unknown"
	 */
	public static String getCliVersion(Path cliPath, Duration timeout) {
		try {
			ProcessResult result = new ProcessExecutor().command(cliPath.toString(), "--version")
				.readOutput(true)
				.redirectErrorStream(true)
				.timeout(timeout.toSeconds(), TimeUnit.SECONDS)
				.exitValue(0)
				.execute();

			return result.outputUTF8().lines().findFirst().orElse("unknown").trim();
		}
		catch (Exception e) {
			return "unknown";
		}
	}

	/** Log environment status without revealing secret values */
	public static void logEnvironmentStatus() {
		System.out.println("Environment status:");
		System.out
			.println("  ANTHROPIC_API_KEY=" + (System.getenv("ANTHROPIC_API_KEY") != null ? "present" : "missing"));
		System.out.println("  GEMINI_API_KEY=" + (System.getenv("GEMINI_API_KEY") != null ? "present" : "missing"));
	}

}
