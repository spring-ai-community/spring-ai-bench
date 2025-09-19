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
package org.springaicommunity.bench.agents.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springaicommunity.bench.core.run.BenchResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates JSON reports for benchmark results.
 */
public class JsonReportGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Path generate(BenchResult result, Path outputDir) throws Exception {
        Path reportDir = outputDir.resolve("bench-reports").resolve(result.benchId());
        Files.createDirectories(reportDir);

        // Create structured report data
        Map<String, Object> report = new LinkedHashMap<>();

        // Basic information
        report.put("benchId", result.benchId());
        report.put("success", result.passed());
        report.put("durationMillis", result.durationMillis());
        report.put("timestamp", Instant.now().toString());

        // Execution details
        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("exitCode", result.passed() ? 0 : 1);
        execution.put("logPath", result.logPath());
        execution.put("completed", true);
        report.put("execution", execution);

        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("generator", "Spring AI Bench");
        metadata.put("version", "0.1.0-SNAPSHOT");
        metadata.put("reportFormat", "1.0");
        metadata.put("generatedAt", Instant.now().toString());
        report.put("metadata", metadata);

        // Environment information
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("javaVersion", System.getProperty("java.version"));
        environment.put("osName", System.getProperty("os.name"));
        environment.put("osVersion", System.getProperty("os.version"));
        environment.put("architecture", System.getProperty("os.arch"));
        report.put("environment", environment);

        // Summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", result.passed() ? "PASSED" : "FAILED");
        summary.put("category", "integration-test");
        summary.put("agent", "hello-world");
        summary.put("duration", result.durationMillis() + "ms");
        report.put("summary", summary);

        // Write JSON report
        Path jsonFile = reportDir.resolve("report.json");
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(jsonFile.toFile(), report);

        return jsonFile;
    }
}