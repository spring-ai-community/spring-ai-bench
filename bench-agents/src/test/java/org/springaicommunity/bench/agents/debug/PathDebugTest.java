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
package org.springaicommunity.bench.agents.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springaicommunity.bench.agents.verifier.VerificationContext;

/** Debug test to understand path calculations in verification */
class PathDebugTest {

	@Test
	void debugPathCalculations() throws IOException {
		// Simulate the same path setup as AgentModelAdapter
		Path tempWorkspace = Files.createTempDirectory("debug-workspace-");

		UUID runId = UUID.randomUUID();
		Path runRoot = tempWorkspace.getParent().resolve("bench-reports").resolve(runId.toString());
		Path actualWorkspace = tempWorkspace;

		System.out.println("=== Original Paths ===");
		System.out.println("tempWorkspace: " + tempWorkspace);
		System.out.println("tempWorkspace.getParent(): " + tempWorkspace.getParent());
		System.out.println("runRoot: " + runRoot);
		System.out.println("actualWorkspace: " + actualWorkspace);

		// Current (broken) way of creating VerificationContext
		System.out.println("\n=== Current (Broken) Approach ===");
		Path relativeWorkspacePath = runRoot.getParent().relativize(actualWorkspace);
		VerificationContext brokenContext = new VerificationContext(runRoot.getParent(), relativeWorkspacePath,
				Instant.now());
		System.out.println("runRoot.getParent(): " + runRoot.getParent());
		System.out.println("relativeWorkspacePath: " + relativeWorkspacePath);
		System.out.println("brokenContext.runRoot(): " + brokenContext.runRoot());
		System.out.println("brokenContext.workspaceRel(): " + brokenContext.workspaceRel());
		Path resolvedWorkspace = brokenContext.runRoot().resolve(brokenContext.workspaceRel());
		System.out.println("resolvedWorkspace: " + resolvedWorkspace);
		System.out.println("resolvedWorkspace equals actualWorkspace: " + resolvedWorkspace.equals(actualWorkspace));

		// What I think it should be
		System.out.println("\n=== Correct Approach ===");
		// The verification context should just point directly to the workspace
		// or use a simple relative path from a consistent base
		VerificationContext correctContext = new VerificationContext(actualWorkspace.getParent(),
				actualWorkspace.getFileName(), Instant.now());
		System.out.println("correctContext.runRoot(): " + correctContext.runRoot());
		System.out.println("correctContext.workspaceRel(): " + correctContext.workspaceRel());
		Path correctResolvedWorkspace = correctContext.runRoot().resolve(correctContext.workspaceRel());
		System.out.println("correctResolvedWorkspace: " + correctResolvedWorkspace);
		System.out.println(
				"correctResolvedWorkspace equals actualWorkspace: " + correctResolvedWorkspace.equals(actualWorkspace));

		// Create a test file
		Files.createDirectories(actualWorkspace);
		Path testFile = actualWorkspace.resolve("hello.txt");
		Files.writeString(testFile, "Hello World!");

		System.out.println("\n=== File Check ===");
		System.out.println("testFile: " + testFile);
		System.out.println("testFile exists: " + Files.exists(testFile));

		// Check if HelloWorldVerifier would find it using correct approach
		Path helloFile = correctResolvedWorkspace.resolve("hello.txt");
		System.out.println("helloFile (correct approach): " + helloFile);
		System.out.println("helloFile exists (correct approach): " + Files.exists(helloFile));

		// Cleanup
		Files.deleteIfExists(testFile);
		Files.deleteIfExists(actualWorkspace);
	}

}
