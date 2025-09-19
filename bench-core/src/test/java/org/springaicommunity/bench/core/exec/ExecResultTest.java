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
package org.springaicommunity.bench.core.exec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExecResultTest {

	@Test
	void success_returnsTrueForZeroExitCode() {
		ExecResult result = new ExecResult(0, "output", Duration.ofSeconds(1));
		assertThat(result.success()).isTrue();
	}

	@Test
	void failed_returnsTrueForNonZeroExitCode() {
		ExecResult result = new ExecResult(1, "error", Duration.ofSeconds(1));
		assertThat(result.failed()).isTrue();
	}

	@Test
	void success_returnsFalseForNonZeroExitCode() {
		ExecResult result = new ExecResult(1, "error", Duration.ofSeconds(1));
		assertThat(result.success()).isFalse();
	}

	@Test
	void failed_returnsFalseForZeroExitCode() {
		ExecResult result = new ExecResult(0, "output", Duration.ofSeconds(1));
		assertThat(result.failed()).isFalse();
	}

	@Test
	void hasOutput_returnsTrueForNonEmptyLog() {
		ExecResult result = new ExecResult(0, "some output", Duration.ofSeconds(1));
		assertThat(result.hasOutput()).isTrue();
	}

	@Test
	void hasOutput_returnsFalseForEmptyLog() {
		ExecResult result = new ExecResult(0, "", Duration.ofSeconds(1));
		assertThat(result.hasOutput()).isFalse();
	}

	@Test
	void outputLength_returnsCorrectLength() {
		String output = "Hello World";
		ExecResult result = new ExecResult(0, output, Duration.ofSeconds(1));
		assertThat(result.outputLength()).isEqualTo(output.length());
	}

	@Test
	void outputLength_returnsZeroForEmptyLog() {
		ExecResult result = new ExecResult(0, "", Duration.ofSeconds(1));
		assertThat(result.outputLength()).isEqualTo(0);
	}

	@Test
	void summary_doesNotIncludeFullOutput() {
		String longOutput = "This is a very long output that should not appear in the summary";
		ExecResult result = new ExecResult(0, longOutput, Duration.ofSeconds(2));
		String summary = result.summary();

		assertThat(summary).contains("exitCode=0");
		assertThat(summary).contains("success=true");
		assertThat(summary).contains("duration=PT2S");
		assertThat(summary).contains("outputLength=" + longOutput.length());
		assertThat(summary).doesNotContain(longOutput);
	}

	@Test
	void summary_showsFailedExecution() {
		ExecResult result = new ExecResult(1, "error message", Duration.ofMillis(500));
		String summary = result.summary();

		assertThat(summary).contains("exitCode=1");
		assertThat(summary).contains("success=false");
		assertThat(summary).contains("duration=PT0.5S");
	}

	@Test
	void toString_includesFullOutput() {
		String output = "Full output content";
		ExecResult result = new ExecResult(0, output, Duration.ofSeconds(1));
		String toString = result.toString();

		assertThat(toString).contains("exitCode=0");
		assertThat(toString).contains("duration=PT1S");
		assertThat(toString).contains(output);
	}

	@Test
	void constructor_requiresNonNullMergedLog() {
		assertThatThrownBy(() -> new ExecResult(0, null, Duration.ofSeconds(1)))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("mergedLog cannot be null");
	}

	@Test
	void constructor_requiresNonNullDuration() {
		assertThatThrownBy(() -> new ExecResult(0, "output", null)).isInstanceOf(NullPointerException.class)
			.hasMessageContaining("duration cannot be null");
	}

	@Test
	void recordProperties_areAccessible() {
		int exitCode = 42;
		String mergedLog = "test output";
		Duration duration = Duration.ofMinutes(1);

		ExecResult result = new ExecResult(exitCode, mergedLog, duration);

		assertThat(result.exitCode()).isEqualTo(exitCode);
		assertThat(result.mergedLog()).isEqualTo(mergedLog);
		assertThat(result.duration()).isEqualTo(duration);
	}

}
