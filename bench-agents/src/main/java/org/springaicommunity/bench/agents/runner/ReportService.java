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
package org.springaicommunity.bench.agents.runner;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.bench.agents.report.IndexPageGenerator;
import org.springaicommunity.bench.agents.report.MinimalHtmlReportGenerator;
import org.springaicommunity.bench.agents.report.MinimalJsonReportGenerator;
import org.springframework.stereotype.Service;

/** Service for generating benchmark reports including JSON, HTML, and index pages. */
@Service
public class ReportService {

	private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

	/**
	 * Generates all reports for a benchmark run.
	 * @param runId the unique run identifier
	 * @param caseId the benchmark case identifier
	 * @param success whether the benchmark was successful
	 * @param startedAt the start time
	 * @param finishedAt the finish time
	 * @param durationMs the duration in milliseconds
	 * @param judgment the judgment result
	 * @param runRoot the run root directory
	 * @param workspace the workspace directory
	 * @param agentResponse the agent response
	 * @param agentModel the agent model used
	 */
	public void generateReports(UUID runId, String caseId, boolean success, Instant startedAt, Instant finishedAt,
			long durationMs, Judgment judgment, Path runRoot, Path workspace, AgentResponse agentResponse,
			AgentModel agentModel) {
		try {
			String agentProviderInfo = getAgentProviderInfo(agentModel);

			// Generate JSON report with enhanced provenance
			MinimalJsonReportGenerator.generate(runId, caseId, success, startedAt, finishedAt, durationMs, judgment,
					runRoot, workspace, agentProviderInfo);

			// Generate HTML report with agent response metadata
			MinimalHtmlReportGenerator.generate(runId, caseId, success, startedAt, finishedAt, durationMs, judgment,
					runRoot, agentResponse);

			// Update index page
			Path reportsBaseDir = runRoot.getParent();
			IndexPageGenerator.generate(reportsBaseDir);

			logger.debug("Successfully generated reports for run: {}", runId);

		}
		catch (Exception e) {
			// Log error but don't fail the execution
			logger.error("Failed to generate reports for run: {}", runId, e);
		}
	}

	/**
	 * Determines the agent provider information based on the agent model class.
	 * @param agentModel the agent model
	 * @return the provider identifier string
	 */
	private String getAgentProviderInfo(AgentModel agentModel) {
		String className = agentModel.getClass().getSimpleName();
		if (className.contains("Claude")) {
			return "claude-code";
		}
		else if (className.contains("Gemini")) {
			return "gemini";
		}
		else {
			return "hello-world";
		}
	}

}
