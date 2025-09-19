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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.agents.hello.HelloWorldVerifier;
import org.springaicommunity.bench.agents.verifier.VerificationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Debug test to understand path resolution differences between different agent setups.
 */
class PathVerificationDebugTest {

    @TempDir
    Path tempDir;

    @Test
    void debugPathResolution() throws Exception {
        // Create a test file in temp directory
        Path testFile = tempDir.resolve("hello.txt");
        Files.writeString(testFile, "Hello World!");

        System.out.println("=== SETUP ===");
        System.out.println("tempDir: " + tempDir);
        System.out.println("tempDir.getParent(): " + tempDir.getParent());
        System.out.println("tempDir.getFileName(): " + tempDir.getFileName());
        System.out.println("testFile: " + testFile);
        System.out.println("testFile exists: " + Files.exists(testFile));

        // Test verification with the pattern AgentModelAdapter uses
        VerificationContext context = new VerificationContext(
            tempDir.getParent(),    // runRoot
            tempDir.getFileName(),  // workspaceRel
            Instant.now()
        );

        System.out.println("=== VERIFICATION CONTEXT ===");
        System.out.println("context.runRoot(): " + context.runRoot());
        System.out.println("context.workspaceRel(): " + context.workspaceRel());

        // Test the verifier
        HelloWorldVerifier verifier = new HelloWorldVerifier();
        verifier.verify(context);
    }
}