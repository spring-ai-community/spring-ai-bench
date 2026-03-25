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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springaicommunity.bench.core.run.BenchResult;

/** Generates HTML reports for benchmark results. */
public class HtmlReportGenerator {

	public static Path generate(BenchResult result, Path outputDir) throws Exception {
		Path reportDir = outputDir.resolve("bench-reports").resolve(result.benchId());
		Files.createDirectories(reportDir);

		String html = """
				<!DOCTYPE html>
				<html>
				<head>
				    <meta charset="UTF-8">
				    <title>Benchmark Report: %s</title>
				    <style>
				        body {
				            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
				            max-width: 1200px;
				            margin: 0 auto;
				            padding: 20px;
				            line-height: 1.6;
				        }
				        .header {
				            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
				            color: white;
				            padding: 30px;
				            border-radius: 12px;
				            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
				        }
				        .header h1 {
				            margin: 0 0 10px 0;
				            font-size: 2.5em;
				        }
				        .status-pass {
				            color: #10b981;
				            font-weight: bold;
				            font-size: 1.2em;
				        }
				        .status-fail {
				            color: #ef4444;
				            font-weight: bold;
				            font-size: 1.2em;
				        }
				        .info-grid {
				            display: grid;
				            grid-template-columns: 200px 1fr;
				            gap: 15px;
				            margin: 30px 0;
				            background: #f8fafc;
				            padding: 25px;
				            border-radius: 8px;
				            border: 1px solid #e2e8f0;
				        }
				        .label {
				            font-weight: 600;
				            color: #374151;
				        }
				        .value {
				            color: #111827;
				        }
				        pre {
				            background: #1f2937;
				            color: #f9fafb;
				            padding: 20px;
				            border-radius: 8px;
				            overflow-x: auto;
				            font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
				        }
				        .section {
				            margin: 30px 0;
				        }
				        .section h2 {
				            color: #374151;
				            border-bottom: 2px solid #e5e7eb;
				            padding-bottom: 10px;
				        }
				        .badge {
				            display: inline-block;
				            padding: 4px 12px;
				            border-radius: 16px;
				            font-size: 0.875em;
				            font-weight: 500;
				        }
				        .badge-success {
				            background: #dcfce7;
				            color: #166534;
				        }
				        .badge-failure {
				            background: #fecaca;
				            color: #991b1b;
				        }
				    </style>
				</head>
				<body>
				    <div class="header">
				        <h1>🚀 Spring AI Bench Report</h1>
				        <div>%s</div>
				        <div class="status-%s">
				            %s
				            <span class="badge badge-%s">%s</span>
				        </div>
				    </div>

				    <div class="section">
				        <h2>📊 Execution Details</h2>
				        <div class="info-grid">
				            <div class="label">Case ID:</div>
				            <div class="value">%s</div>

				            <div class="label">Status:</div>
				            <div class="value status-%s">%s</div>

				            <div class="label">Duration:</div>
				            <div class="value">%d ms</div>

				            <div class="label">Timestamp:</div>
				            <div class="value">%s</div>

				            <div class="label">Log Path:</div>
				            <div class="value">%s</div>
				        </div>
				    </div>

				    <div class="section">
				        <h2>📁 Workspace Information</h2>
				        <pre>Workspace: Not available in current BenchResult</pre>
				    </div>

				    <div class="section">
				        <h2>🔗 Next Steps</h2>
				        <ul>
				            <li>Review the execution details above</li>
				            <li>Check the workspace for generated files</li>
				            <li>View the JSON report for machine-readable data</li>
				            <li>Run additional benchmarks as needed</li>
				        </ul>
				    </div>

				    <footer style="margin-top: 50px; text-align: center; color: #6b7280; font-size: 0.875em;">
				        Generated by Spring AI Bench • %s
				    </footer>
				</body>
				</html>
				""".formatted(result.benchId(), // title
				result.benchId(), // header case name
				result.passed() ? "pass" : "fail", // status class
				result.passed() ? "✅ PASSED" : "❌ FAILED", // status text
				result.passed() ? "success" : "failure", // badge class
				result.passed() ? "PASSED" : "FAILED", // badge text
				result.benchId(), // case ID
				result.passed() ? "pass" : "fail", // status class 2
				result.passed() ? "PASSED" : "FAILED", // status text 2
				result.durationMillis(), // duration
				DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( // timestamp
						Instant.now().atZone(ZoneId.systemDefault())),
				result.logPath() != null ? result.logPath() : "No log file", // log path
				DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( // footer timestamp
						Instant.now().atZone(ZoneId.systemDefault())));

		Path htmlFile = reportDir.resolve("index.html");
		Files.writeString(htmlFile, html);
		return htmlFile;
	}

}
