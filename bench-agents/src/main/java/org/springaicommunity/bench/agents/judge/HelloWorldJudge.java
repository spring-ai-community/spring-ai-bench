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
package org.springaicommunity.bench.agents.judge;

import org.springaicommunity.agents.judge.DeterministicJudge;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.fs.FileContentJudge;
import org.springaicommunity.agents.judge.fs.FileExistsJudge;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.judge.score.BooleanScore;

/**
 * Judge for Hello World benchmark case. Verifies that hello.txt exists and contains
 * exactly "Hello World!".
 */
public class HelloWorldJudge extends DeterministicJudge {

	private static final String EXPECTED_CONTENT = "Hello World!";

	private static final String FILE_PATH = "hello.txt";

	private final FileExistsJudge fileExistsJudge;

	private final FileContentJudge fileContentJudge;

	public HelloWorldJudge() {
		super("HelloWorldJudge", "Verifies hello.txt exists with correct content");
		this.fileExistsJudge = new FileExistsJudge(FILE_PATH);
		this.fileContentJudge = new FileContentJudge(FILE_PATH, EXPECTED_CONTENT, FileContentJudge.MatchMode.EXACT);
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		// First check if file exists
		Judgment existsJudgment = fileExistsJudge.judge(context);
		if (!existsJudgment.pass()) {
			return existsJudgment;
		}

		// Then check content
		Judgment contentJudgment = fileContentJudge.judge(context);
		if (!contentJudgment.pass()) {
			return contentJudgment;
		}

		// Both passed
		return Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning("File exists with correct content")
			.checks(contentJudgment.checks())
			.build();
	}

}
