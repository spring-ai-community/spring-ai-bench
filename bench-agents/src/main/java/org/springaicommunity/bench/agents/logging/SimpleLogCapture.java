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
package org.springaicommunity.bench.agents.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Simple timestamped logging for agent execution.
 * Writes to run.log with ISO timestamps and clear markers.
 */
public class SimpleLogCapture {

    private final Path logFile;
    private final String runId;

    public SimpleLogCapture(Path runRoot, UUID runId) throws IOException {
        this.runId = runId.toString();
        this.logFile = runRoot.resolve("run.log");

        // Ensure parent directory exists
        Files.createDirectories(runRoot);

        // Write header
        writeHeader();
    }

    private void writeHeader() throws IOException {
        log("RUN_ID: " + runId);
        log("STARTED: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
    }

    public void writeFooter() throws IOException {
        log("FINISHED: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
    }

    public void log(String category, String message) throws IOException {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String line = String.format("%s [%s] %s%n", timestamp, category, message);

        Files.writeString(logFile, line,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }

    public void log(String message) throws IOException {
        String line = message + System.lineSeparator();

        Files.writeString(logFile, line,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }

    public Path getLogFile() {
        return logFile;
    }
}