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
package org.springaicommunity.bench.core.exec.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.bench.core.exec.*;
import org.springaicommunity.bench.core.exec.customizer.ExecSpecCustomizer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

/**
 * Sandbox implementation that executes commands in local processes within an isolated directory.
 * <p>
 * Supports {@link ExecSpecCustomizer}s for last-mile command and environment customization.
 * Each execution applies all customizers in sequence before launching the process.
 * <p>
 * <strong>Security Note:</strong> This implementation provides directory isolation only.
 * Commands execute with the same privileges as the JVM process.
 */
public final class LocalSandbox implements Sandbox {

    private static final Logger logger = LoggerFactory.getLogger(LocalSandbox.class);

    private final Path workingDirectory;
    private final List<ExecSpecCustomizer> customizers;
    private volatile boolean closed = false;

    private LocalSandbox(Path workingDirectory, List<ExecSpecCustomizer> customizers) throws IOException {
        this.workingDirectory = workingDirectory;
        this.customizers = List.copyOf(customizers);
        Files.createDirectories(workingDirectory);
        logger.debug("Created LocalSandbox with working directory: {}", workingDirectory);
    }

    /**
     * Returns a new Builder for creating a LocalSandbox instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Path workDir() {
        return workingDirectory;
    }

    @Override
    public ExecResult exec(ExecSpec spec) throws IOException, InterruptedException, TimeoutException {
        if (closed) {
            throw new IllegalStateException("Sandbox has been closed");
        }

        // Apply all customizers in sequence
        ExecSpec customizedSpec = spec;
        for (ExecSpecCustomizer customizer : customizers) {
            customizedSpec = customizer.customize(customizedSpec);
        }

        if (customizedSpec.command().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty after customization");
        }

        // Handle shell commands by wrapping them appropriately for the platform
        List<String> actualCommand = customizedSpec.command();
        if (actualCommand.size() == 2 && "__SHELL_COMMAND__".equals(actualCommand.get(0))) {
            actualCommand = wrapShellCommand(actualCommand.get(1));
        }

        logger.debug("Executing customized command: {} in {}",
                actualCommand, workingDirectory);

        // Build process environment (defensive copy to avoid mutation)
        Map<String, String> processEnv = new HashMap<>(customizedSpec.env());

        // Add MCP tools environment variable if present
        if (customizedSpec.mcp() != null && !customizedSpec.mcp().servers().isEmpty()) {
            processEnv.put("MCP_TOOLS", String.join(",", customizedSpec.mcp().servers()));
            logger.debug("Added MCP_TOOLS environment variable: {}", processEnv.get("MCP_TOOLS"));
        }

        // Execute process using zt-exec
        ProcessExecutor executor = new ProcessExecutor()
                .command(actualCommand)
                .directory(workingDirectory.toFile())
                .environment(processEnv)
                .readOutput(true)
                .redirectErrorStream(true); // Merge stderr into stdout for chronological ordering

        // Add timeout if specified
        if (customizedSpec.timeout() != null) {
            executor = executor.timeout(customizedSpec.timeout().toMillis(), TimeUnit.MILLISECONDS);
        }

        long startTime = System.nanoTime();
        try {
            ProcessResult result = executor.execute();
            Duration executionDuration = Duration.ofNanos(System.nanoTime() - startTime);

            return new ExecResult(
                    result.getExitValue(),
                    result.outputUTF8(),
                    executionDuration
            );
        } catch (java.util.concurrent.TimeoutException e) {
            String timeoutDuration = customizedSpec.timeout() != null ? customizedSpec.timeout().toString() : "PT0S";
            throw new TimeoutException("Process timed out after " + timeoutDuration);
        }

    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        // Only delete directories that start with our temp prefix (that we created)
        if (workingDirectory.getFileName().toString().startsWith("sai-bench-")) {
            logger.debug("Closing sandbox and deleting temporary directory: {}", workingDirectory);
            try {
                // Recursively delete the directory and its contents
                if (Files.exists(workingDirectory)) {
                Files.walkFileTree(workingDirectory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                }
            } catch (IOException e) {
                // Log the error but don't rethrow, as close() shouldn't fail loudly.
                logger.warn("Could not completely delete sandbox directory: {}", workingDirectory, e);
            }
        } else {
            logger.debug("Closing sandbox, keeping external directory: {}", workingDirectory);
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * Wrap a shell command string in the appropriate shell for the current platform.
     */
    private static List<String> wrapShellCommand(String shellCommand) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: use cmd /c
            return List.of("cmd", "/c", shellCommand);
        } else {
            // Unix-like: use bash -c (or sh -c as fallback)
            return List.of("bash", "-c", shellCommand);
        }
    }

    /**
     * Builder for {@link LocalSandbox}.
     */
    public static final class Builder {

        private Path workingDirectory;
        private final List<ExecSpecCustomizer> customizers = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets a specific working directory for the sandbox.
         * <p>
         * If not set, a new temporary directory will be created.
         *
         * @param workingDirectory the directory to use.
         * @return this builder.
         */
        public Builder workingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        /**
         * Adds a list of customizers to be applied to every {@link ExecSpec}.
         *
         * @param customizers the customizers to add.
         * @return this builder.
         */
        public Builder customizers(List<ExecSpecCustomizer> customizers) {
            this.customizers.addAll(customizers);
            return this;
        }

        /**
         * Adds a single customizer to be applied to every {@link ExecSpec}.
         *
         * @param customizer the customizer to add.
         * @return this builder.
         */
        public Builder customizer(ExecSpecCustomizer customizer) {
            this.customizers.add(customizer);
            return this;
        }

        /**
         * Builds the {@link LocalSandbox} instance.
         *
         * @return a new {@link LocalSandbox}.
         * @throws UncheckedIOException if the working directory cannot be created.
         */
        public LocalSandbox build() {
            try {
                Path dir = (this.workingDirectory != null) ? this.workingDirectory : Files.createTempDirectory("sai-bench-");
                return new LocalSandbox(dir, customizers);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create working directory for sandbox", e);
            }
        }
    }
}
