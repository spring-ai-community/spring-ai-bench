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
package org.springaicommunity.bench.agents.claudecode;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Direct integration test with Claude CLI for hello world task.
 * This test directly calls the Claude CLI to verify it can create hello.txt.
 */
@Tag("agents-live")
@Tag("claude")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@Timeout(180)  // Cap runtime at 3 minutes for Claude execution
class ClaudeCodeDirectIntegrationTest {

    private Path tempWorkspace;

    @BeforeAll
    static void requireCli() {
        // Check if claude CLI is available
        assumeTrue(isCliAvailable("claude"), "Claude CLI not available on PATH");
    }

    @BeforeEach
    void setUp() throws Exception {
        tempWorkspace = Files.createTempDirectory("claude-direct-test-");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempWorkspace != null && Files.exists(tempWorkspace)) {
            Files.walk(tempWorkspace)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // Best effort cleanup
                    }
                });
        }
    }

    @Test
    void claudeCli_canCreateHelloWorldFile() throws Exception {
        String prompt = "Create a file named hello.txt in the current working directory with EXACT contents: Hello World!";

        // Execute Claude CLI directly with zt-exec
        ProcessResult result = new ProcessExecutor()
            .command("claude", prompt, "--dangerously-skip-permissions")
            .directory(tempWorkspace.toFile())
            .readOutput(true)
            .timeout(120, TimeUnit.SECONDS)
            .execute();

        // Verify Claude executed successfully
        System.out.println("Claude CLI Output:");
        System.out.println(result.outputUTF8());

        // Verify the hello.txt file was created
        Path helloFile = tempWorkspace.resolve("hello.txt");
        assertThat(helloFile).exists();

        // Verify the content is correct
        String content = Files.readString(helloFile);
        assertThat(content.trim()).isEqualTo("Hello World!");

        System.out.println("✅ SUCCESS: Claude CLI created hello.txt with correct content");
    }

    @Test
    void claudeCli_versionCheck() throws Exception {
        // Test that we can get Claude version
        ProcessResult result = new ProcessExecutor()
            .command("claude", "--version")
            .readOutput(true)
            .timeout(10, TimeUnit.SECONDS)
            .execute();

        assertThat(result.getExitValue()).isEqualTo(0);

        String version = result.outputUTF8().trim();
        assertThat(version).isNotEmpty();
        assertThat(version).contains("Claude Code");

        System.out.println("Claude CLI Version: " + version);
    }

    private static boolean isCliAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "--version")
                .redirectErrorStream(true)
                .start();
            boolean finished = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}