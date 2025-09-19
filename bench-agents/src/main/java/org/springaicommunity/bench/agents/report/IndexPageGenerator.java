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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates an index page listing all agent runs.
 * Shows recent runs with status, duration, and links.
 */
public class IndexPageGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Path generate(Path reportsBaseDir) throws IOException {
        List<RunSummary> runs = collectRunSummaries(reportsBaseDir);

        // Sort by timestamp, newest first
        runs.sort(Comparator.comparing(RunSummary::startedAt).reversed());

        // Take last 20 runs for performance
        List<RunSummary> recentRuns = runs.stream().limit(20).toList();

        String html = generateIndexHtml(recentRuns);

        Path indexFile = reportsBaseDir.resolve("index.html");
        Files.writeString(indexFile, html);

        return indexFile;
    }

    private static List<RunSummary> collectRunSummaries(Path reportsBaseDir) throws IOException {
        List<RunSummary> runs = new ArrayList<>();

        if (!Files.exists(reportsBaseDir)) {
            return runs;
        }

        Files.list(reportsBaseDir)
            .filter(Files::isDirectory)
            .forEach(runDir -> {
                try {
                    Path reportJson = runDir.resolve("report.json");
                    if (Files.exists(reportJson)) {
                        JsonNode report = objectMapper.readTree(reportJson.toFile());

                        runs.add(new RunSummary(
                            report.get("runId").asText(),
                            report.get("caseId").asText(),
                            report.get("success").asBoolean(),
                            report.get("durationMs").asLong(),
                            Instant.parse(report.get("startedAt").asText()),
                            report.has("reason") ? report.get("reason").asText() : "No reason",
                            runDir.getFileName().toString()
                        ));
                    }
                } catch (Exception e) {
                    // Skip runs with corrupted reports
                }
            });

        return runs;
    }

    private static String generateIndexHtml(List<RunSummary> runs) {
        StringBuilder tableRows = new StringBuilder();

        if (runs.isEmpty()) {
            tableRows.append("<tr><td colspan=\"6\" style=\"text-align: center; color: #6b7280; font-style: italic;\">No runs found</td></tr>");
        } else {
            for (RunSummary run : runs) {
                String statusBadge = run.success() ?
                    "<span class=\"badge success\">✅ PASS</span>" :
                    "<span class=\"badge failure\">❌ FAIL</span>";

                String timestamp = DateTimeFormatter.ISO_INSTANT.format(run.startedAt());

                tableRows.append("<tr>");
                tableRows.append("<td><a href=\"").append(run.dirName()).append("/index.html\">").append(run.runId().substring(0, 8)).append("...</a></td>");
                tableRows.append("<td>").append(run.caseId()).append("</td>");
                tableRows.append("<td>").append(statusBadge).append("</td>");
                tableRows.append("<td>").append(run.durationMs()).append(" ms</td>");
                tableRows.append("<td>").append(timestamp).append("</td>");
                tableRows.append("<td>").append(run.reason()).append("</td>");
                tableRows.append("</tr>");
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Spring AI Bench - Agent Runs</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        max-width: 1400px;
                        margin: 0 auto;
                        padding: 20px;
                        line-height: 1.6;
                        background: #f8fafc;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 40px;
                        border-radius: 12px;
                        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
                        margin-bottom: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 2.5em;
                    }
                    .header p {
                        margin: 10px 0 0 0;
                        opacity: 0.9;
                        font-size: 1.1em;
                    }
                    .content {
                        background: white;
                        border-radius: 12px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                        overflow: hidden;
                    }
                    .runs-table {
                        width: 100%%;
                        border-collapse: collapse;
                    }
                    .runs-table th, .runs-table td {
                        padding: 15px 20px;
                        text-align: left;
                        border-bottom: 1px solid #e5e7eb;
                    }
                    .runs-table th {
                        background: #f9fafb;
                        font-weight: 600;
                        color: #374151;
                        font-size: 0.9em;
                        text-transform: uppercase;
                        letter-spacing: 0.05em;
                    }
                    .runs-table tr:hover {
                        background: #f9fafb;
                    }
                    .runs-table a {
                        color: #3b82f6;
                        text-decoration: none;
                        font-family: 'SF Mono', Monaco, monospace;
                    }
                    .runs-table a:hover {
                        text-decoration: underline;
                    }
                    .badge {
                        display: inline-block;
                        padding: 4px 12px;
                        border-radius: 16px;
                        font-size: 0.85em;
                        font-weight: 500;
                    }
                    .badge.success {
                        background: #dcfce7;
                        color: #166534;
                    }
                    .badge.failure {
                        background: #fecaca;
                        color: #991b1b;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 40px;
                        color: #6b7280;
                        font-size: 0.9em;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🚀 Spring AI Bench</h1>
                    <p>Agent Execution Dashboard</p>
                </div>

                <div class="content">
                    <table class="runs-table">
                        <thead>
                            <tr>
                                <th>Run ID</th>
                                <th>Case</th>
                                <th>Status</th>
                                <th>Duration</th>
                                <th>Started</th>
                                <th>Result</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <div class="footer">
                    Generated by Spring AI Bench • %s<br>
                    Last %d runs shown
                </div>
            </body>
            </html>
            """.formatted(
                tableRows.toString(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                runs.size()
            );
    }

    private record RunSummary(
        String runId,
        String caseId,
        boolean success,
        long durationMs,
        Instant startedAt,
        String reason,
        String dirName
    ) {}
}