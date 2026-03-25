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

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for TimeoutException functionality. */
class TimeoutExceptionTest {

	@Nested
	@DisplayName("Constructor tests")
	class ConstructorTests {

		@Test
		@DisplayName("should create exception with simple message")
		void constructor_withMessage() {
			String message = "Operation timed out";

			TimeoutException exception = new TimeoutException(message);

			assertThat(exception.getMessage()).isEqualTo(message);
			assertThat(exception.getTimeout()).isNull();
			assertThat(exception.getElapsed()).isNull();
			assertThat(exception.hasTimeoutDetails()).isFalse();
		}

		@Test
		@DisplayName("should create exception with message and timeout")
		void constructor_withMessageAndTimeout() {
			String message = "Command execution timed out";
			Duration timeout = Duration.ofMinutes(5);

			TimeoutException exception = new TimeoutException(message, timeout);

			assertThat(exception.getMessage()).isEqualTo(message);
			assertThat(exception.getTimeout()).isEqualTo(timeout);
			assertThat(exception.getElapsed()).isNull();
			assertThat(exception.hasTimeoutDetails()).isTrue();
		}

		@Test
		@DisplayName("should create exception with message, timeout, and elapsed time")
		void constructor_withAllDetails() {
			String message = "Process exceeded time limit";
			Duration timeout = Duration.ofMinutes(5);
			Duration elapsed = Duration.ofMinutes(6);

			TimeoutException exception = new TimeoutException(message, timeout, elapsed);

			assertThat(exception.getMessage()).isEqualTo(message);
			assertThat(exception.getTimeout()).isEqualTo(timeout);
			assertThat(exception.getElapsed()).isEqualTo(elapsed);
			assertThat(exception.hasTimeoutDetails()).isTrue();
		}

		@Test
		@DisplayName("should create exception with message and cause")
		void constructor_withMessageAndCause() {
			String message = "Timeout occurred";
			Exception cause = new InterruptedException("Process interrupted");

			TimeoutException exception = new TimeoutException(message, cause);

			assertThat(exception.getMessage()).isEqualTo(message);
			assertThat(exception.getCause()).isEqualTo(cause);
			assertThat(exception.getTimeout()).isNull();
			assertThat(exception.getElapsed()).isNull();
			assertThat(exception.hasTimeoutDetails()).isFalse();
		}

	}

	@Nested
	@DisplayName("Timeout details handling")
	class TimeoutDetailsTests {

		@Test
		@DisplayName("should indicate when timeout details are available")
		void hasTimeoutDetails_returnsTrueWhenTimeoutProvided() {
			TimeoutException exception = new TimeoutException("test", Duration.ofSeconds(30));

			assertThat(exception.hasTimeoutDetails()).isTrue();
		}

		@Test
		@DisplayName("should indicate when timeout details are not available")
		void hasTimeoutDetails_returnsFalseWhenNoTimeout() {
			TimeoutException exception = new TimeoutException("test");

			assertThat(exception.hasTimeoutDetails()).isFalse();
		}

		@Test
		@DisplayName("should handle null timeout gracefully")
		void hasTimeoutDetails_handleNullTimeout() {
			TimeoutException exception = new TimeoutException("test", (Duration) null);

			assertThat(exception.hasTimeoutDetails()).isFalse();
			assertThat(exception.getTimeout()).isNull();
		}

	}

	@Nested
	@DisplayName("Detailed message formatting")
	class DetailedMessageTests {

		@Test
		@DisplayName("should return simple message when no timeout details")
		void getDetailedMessage_returnsSimpleMessageWhenNoDetails() {
			String message = "Operation failed";
			TimeoutException exception = new TimeoutException(message);

			String detailedMessage = exception.getDetailedMessage();

			assertThat(detailedMessage).isEqualTo(message);
		}

		@Test
		@DisplayName("should include timeout in detailed message")
		void getDetailedMessage_includesTimeout() {
			String message = "Command timed out";
			Duration timeout = Duration.ofMinutes(2);
			TimeoutException exception = new TimeoutException(message, timeout);

			String detailedMessage = exception.getDetailedMessage();

			assertThat(detailedMessage).isEqualTo("Command timed out (timeout: PT2M)");
		}

		@Test
		@DisplayName("should include timeout and elapsed time in detailed message")
		void getDetailedMessage_includesTimeoutAndElapsed() {
			String message = "Process exceeded limit";
			Duration timeout = Duration.ofMinutes(5);
			Duration elapsed = Duration.ofMinutes(6).plusSeconds(30);
			TimeoutException exception = new TimeoutException(message, timeout, elapsed);

			String detailedMessage = exception.getDetailedMessage();

			assertThat(detailedMessage).isEqualTo("Process exceeded limit (timeout: PT5M, elapsed: PT6M30S)");
		}

		@Test
		@DisplayName("should handle timeout without elapsed time")
		void getDetailedMessage_handlesTimeoutWithoutElapsed() {
			TimeoutException exception = new TimeoutException("test", Duration.ofSeconds(30), null);

			String detailedMessage = exception.getDetailedMessage();

			assertThat(detailedMessage).isEqualTo("test (timeout: PT30S)");
		}

	}

	@Nested
	@DisplayName("toString() behavior")
	class ToStringTests {

		@Test
		@DisplayName("should format toString with class name and detailed message")
		void toString_includesClassNameAndDetailedMessage() {
			TimeoutException exception = new TimeoutException("test", Duration.ofSeconds(30));

			String toString = exception.toString();

			assertThat(toString).startsWith("TimeoutException: ").contains("test (timeout: PT30S)");
		}

		@Test
		@DisplayName("should handle simple message in toString")
		void toString_handlesSimpleMessage() {
			TimeoutException exception = new TimeoutException("simple error");

			String toString = exception.toString();

			assertThat(toString).isEqualTo("TimeoutException: simple error");
		}

	}

	@Nested
	@DisplayName("Duration handling edge cases")
	class DurationEdgeCasesTests {

		@Test
		@DisplayName("should handle zero duration timeout")
		void handleZeroDurationTimeout() {
			TimeoutException exception = new TimeoutException("test", Duration.ZERO);

			assertThat(exception.getTimeout()).isEqualTo(Duration.ZERO);
			assertThat(exception.hasTimeoutDetails()).isTrue();
			assertThat(exception.getDetailedMessage()).contains("timeout: PT0S");
		}

		@Test
		@DisplayName("should handle very large duration")
		void handleLargeDuration() {
			Duration largeDuration = Duration.ofDays(365);
			TimeoutException exception = new TimeoutException("test", largeDuration);

			assertThat(exception.getTimeout()).isEqualTo(largeDuration);
			assertThat(exception.getDetailedMessage()).contains("timeout: PT8760H");
		}

		@Test
		@DisplayName("should handle negative duration gracefully")
		void handleNegativeDuration() {
			Duration negativeDuration = Duration.ofSeconds(-30);
			TimeoutException exception = new TimeoutException("test", negativeDuration);

			assertThat(exception.getTimeout()).isEqualTo(negativeDuration);
			assertThat(exception.getDetailedMessage()).contains("timeout: PT-30S");
		}

		@Test
		@DisplayName("should handle elapsed time greater than timeout")
		void handleElapsedGreaterThanTimeout() {
			Duration timeout = Duration.ofMinutes(5);
			Duration elapsed = Duration.ofMinutes(10);
			TimeoutException exception = new TimeoutException("test", timeout, elapsed);

			assertThat(exception.getElapsed()).isGreaterThan(exception.getTimeout());
			assertThat(exception.getDetailedMessage()).contains("timeout: PT5M").contains("elapsed: PT10M");
		}

	}

	@Nested
	@DisplayName("Integration with standard exception behavior")
	class StandardExceptionBehaviorTests {

		@Test
		@DisplayName("should support exception chaining")
		void supportsExceptionChaining() {
			Exception cause = new RuntimeException("root cause");
			TimeoutException exception = new TimeoutException("timeout occurred", cause);

			assertThat(exception.getCause()).isEqualTo(cause);
			assertThat(exception.getSuppressed()).isEmpty();
		}

		@Test
		@DisplayName("should support stack trace")
		void supportsStackTrace() {
			TimeoutException exception = new TimeoutException("test");

			StackTraceElement[] stackTrace = exception.getStackTrace();

			assertThat(stackTrace).isNotEmpty();
			assertThat(stackTrace[0].getMethodName()).isEqualTo("supportsStackTrace");
		}

		@Test
		@DisplayName("should be throwable and catchable")
		void isThrowableAndCatchable() {
			Duration timeout = Duration.ofSeconds(1);

			assertThatThrownBy(() -> {
				throw new TimeoutException("test timeout", timeout);
			}).isInstanceOf(TimeoutException.class).hasMessage("test timeout").satisfies(ex -> {
				TimeoutException timeoutEx = (TimeoutException) ex;
				assertThat(timeoutEx.getTimeout()).isEqualTo(timeout);
			});
		}

	}

}
