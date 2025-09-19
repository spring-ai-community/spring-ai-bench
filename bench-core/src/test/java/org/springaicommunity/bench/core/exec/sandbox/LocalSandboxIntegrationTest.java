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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.bench.core.exec.*;
import org.springaicommunity.bench.core.exec.customizer.ExecSpecCustomizer;

/**
 * Integration tests for LocalSandbox using real process execution. These tests validate
 * the actual behavior with real commands, timeouts, and file I/O.
 */
class LocalSandboxIntegrationTest {

	@TempDir
	Path tempDir;

	private LocalSandbox sandbox;

	@BeforeEach
	void setUp() {
		// Create a fresh sandbox for each test
		sandbox = LocalSandbox.builder().workingDirectory(tempDir).build();
	}

	@Nested
	@DisplayName("Basic command execution")
	class BasicCommandExecutionTests {

		@Test
		@DisplayName("should execute simple echo command successfully")
		void exec_executesEchoCommand() throws Exception {
			ExecSpec spec = ExecSpec.of("echo", "Hello, World!");

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.exitCode()).isEqualTo(0);
			assertThat(result.mergedLog()).contains("Hello, World!");
			assertThat(result.duration()).isGreaterThan(Duration.ZERO);
		}

		@Test
		@DisplayName("should execute command that fails with non-zero exit code")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_handlesFailingCommand() throws Exception {
			// 'false' command always exits with code 1
			ExecSpec spec = ExecSpec.of("false");

			ExecResult result = sandbox.exec(spec);

			assertThat(result.failed()).isTrue();
			assertThat(result.exitCode()).isEqualTo(1);
			assertThat(result.duration()).isGreaterThan(Duration.ZERO);
		}

		@Test
		@DisplayName("should execute command with multiple arguments")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_handlesMultipleArguments() throws Exception {
			ExecSpec spec = ExecSpec.of("ls", "-la", tempDir.toString());

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains("total");
			assertThat(result.mergedLog()).contains(".");
			assertThat(result.mergedLog()).contains("..");
		}

		@Test
		@DisplayName("should execute command with environment variables")
		void exec_setsEnvironmentVariables() throws Exception {
			ExecSpec spec = ExecSpec.builder()
				.command("sh", "-c", "echo $TEST_VAR")
				.env("TEST_VAR", "integration_test_value")
				.build();

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains("integration_test_value");
		}

		@Test
		@DisplayName("should execute command with MCP tools environment variable")
		void exec_setsMcpToolsEnvironment() throws Exception {
			ExecSpec spec = ExecSpec.builder()
				.command("sh", "-c", "echo $MCP_TOOLS")
				.mcp(McpConfig.of("brave", "filesystem"))
				.build();

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains("brave,filesystem");
		}

	}

	@Nested
	@DisplayName("File I/O operations")
	class FileIOTests {

		@Test
		@DisplayName("should work with file creation and reading")
		void exec_handlesFileOperations() throws Exception {
			// Create a file
			ExecSpec createSpec = ExecSpec.of("sh", "-c", "echo 'test content' > test.txt");
			ExecResult createResult = sandbox.exec(createSpec);

			assertThat(createResult.success()).isTrue();

			// Verify file was created in working directory
			Path testFile = tempDir.resolve("test.txt");
			assertThat(Files.exists(testFile)).isTrue();

			// Read the file back
			ExecSpec readSpec = ExecSpec.of("cat", "test.txt");
			ExecResult readResult = sandbox.exec(readSpec);

			assertThat(readResult.success()).isTrue();
			assertThat(readResult.mergedLog()).contains("test content");
		}

		@Test
		@DisplayName("should work with multiple file operations in sequence")
		void exec_handlesMultipleFileOperations() throws Exception {
			// Create multiple files
			sandbox.exec(ExecSpec.of("sh", "-c", "echo 'file1' > file1.txt"));
			sandbox.exec(ExecSpec.of("sh", "-c", "echo 'file2' > file2.txt"));
			sandbox.exec(ExecSpec.of("mkdir", "subdir"));
			sandbox.exec(ExecSpec.of("sh", "-c", "echo 'file3' > subdir/file3.txt"));

			// List all files
			ExecSpec listSpec = ExecSpec.of("find", ".", "-type", "f");
			ExecResult listResult = sandbox.exec(listSpec);

			assertThat(listResult.success()).isTrue();
			String output = listResult.mergedLog();
			assertThat(output).contains("file1.txt");
			assertThat(output).contains("file2.txt");
			assertThat(output).contains("subdir/file3.txt");
		}

		@Test
		@DisplayName("should handle file operations with special characters")
		void exec_handlesSpecialCharacters() throws Exception {
			String specialContent = "Content with spaces, symbols: !@#$%^&*()";

			ExecSpec spec = ExecSpec.of("sh", "-c",
					String.format("echo '%s' > special.txt && cat special.txt", specialContent));

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains(specialContent);
		}

		@Test
		@DisplayName("should handle binary file operations")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_handlesBinaryFiles() throws Exception {
			// Create a binary file using dd
			ExecSpec createSpec = ExecSpec.of("dd", "if=/dev/zero", "of=binary.dat", "bs=1024", "count=1");
			ExecResult createResult = sandbox.exec(createSpec);

			assertThat(createResult.success()).isTrue();

			// Check file size
			ExecSpec sizeSpec = ExecSpec.of("wc", "-c", "binary.dat");
			ExecResult sizeResult = sandbox.exec(sizeSpec);

			assertThat(sizeResult.success()).isTrue();
			assertThat(sizeResult.mergedLog()).contains("1024");
		}

	}

	@Nested
	@DisplayName("Timeout handling")
	class TimeoutTests {

		@Test
		@DisplayName("should throw TimeoutException when command exceeds timeout")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_throwsTimeoutWhenExceeded() {
			ExecSpec spec = ExecSpec.builder().command("sleep", "2").timeout(Duration.ofMillis(100)).build();

			assertThatThrownBy(() -> sandbox.exec(spec)).isInstanceOf(TimeoutException.class)
				.hasMessageContaining("Process timed out after PT0.1S");
		}

		@Test
		@DisplayName("should complete successfully when command is within timeout")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_completesWithinTimeout() {
			ExecSpec spec = ExecSpec.builder().command("sleep", "0.1").timeout(Duration.ofSeconds(2)).build();

			assertThatCode(() -> sandbox.exec(spec)).doesNotThrowAnyException();
		}

	}

	@Nested
	@DisplayName("Environment variable handling")
	class EnvironmentVariableTests {

		@Test
		@DisplayName("should set environment variables for the process")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_setsEnvironmentVariables() throws Exception {
			ExecSpec spec = ExecSpec.builder()
				.command("sh", "-c", "echo $TEST_VAR")
				.env("TEST_VAR", "hello_env")
				.build();

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains("hello_env");
		}

		@Test
		@DisplayName("should set MCP_TOOLS from McpConfig")
		@EnabledOnOs({ OS.LINUX, OS.MAC })
		void exec_setsMcpToolsFromMcpConfig() throws Exception {
			ExecSpec spec = ExecSpec.builder()
				.command("sh", "-c", "echo $MCP_TOOLS")
				.mcp(McpConfig.of("git", "mvn"))
				.build();

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains("git,mvn");
		}

	}

	@Nested
	@DisplayName("File I/O and working directory")
	class FileIoTests {

		@Test
		@DisplayName("should create and write to a file in the working directory")
		void exec_writesToSandboxFile() throws Exception {
			// Command to write 'file content' into 'output.txt'
			ExecSpec spec = ExecSpec.builder().command("sh", "-c", "echo -n file content > output.txt").build();

			ExecResult result = sandbox.exec(spec);

			assertThat(result.success()).isTrue();

			Path outputFile = tempDir.resolve("output.txt");
			assertThat(outputFile).exists();
			assertThat(Files.readString(outputFile)).isEqualTo("file content");
		}

		@Test
		@DisplayName("should NOT delete external working directory on close")
		void close_doesNotDeleteExternalWorkingDirectory() throws IOException {
			// Ensure the directory exists before closing
			assertThat(tempDir).exists();

			sandbox.close();

			// Assert the directory is NOT deleted (since LocalSandbox didn't create it)
			assertThat(tempDir).exists();
		}

	}

	@Nested
	@DisplayName("Customizer application")
	class CustomizerTests {

		@Test
		@DisplayName("should apply a customizer to modify the command")
		void exec_appliesCustomizer() throws Exception {
			// A customizer that changes the text to be echoed
			ExecSpecCustomizer customizer = spec -> spec.toBuilder().command("echo", "Customized Hello").build();

			LocalSandbox customSandbox = LocalSandbox.builder()
				.workingDirectory(tempDir)
				.customizer(customizer)
				.build();

			ExecSpec originalSpec = ExecSpec.of("echo", "Original Hello");

			ExecResult result = customSandbox.exec(originalSpec);

			assertThat(result.success()).isTrue();
			assertThat(result.mergedLog()).contains("Customized Hello");
			assertThat(result.mergedLog()).doesNotContain("Original Hello");

			customSandbox.close();
		}

		@Test
		@DisplayName("should correctly apply a conditional customizer")
		void exec_appliesConditionalCustomizer() throws Exception {
			ExecSpecCustomizer customizer = spec -> spec.toBuilder().command("echo", "Conditional Hello").build();

			// This customizer will only apply if the command is 'test'
			ExecSpecCustomizer conditionalCustomizer = ExecSpecCustomizer.when(spec -> spec.command().contains("test"),
					customizer);

			LocalSandbox customSandbox = LocalSandbox.builder()
				.workingDirectory(tempDir)
				.customizer(conditionalCustomizer)
				.build();

			// Case 1: Predicate is false, customizer should not run
			ExecSpec spec1 = ExecSpec.of("echo", "No-op");
			ExecResult result1 = customSandbox.exec(spec1);
			assertThat(result1.mergedLog()).contains("No-op");
			assertThat(result1.mergedLog()).doesNotContain("Conditional Hello");

			// Case 2: Predicate is true, customizer should run
			ExecSpec spec2 = ExecSpec.of("test"); // The command the predicate looks for
			ExecResult result2 = customSandbox.exec(spec2);
			assertThat(result2.mergedLog()).contains("Conditional Hello");

			customSandbox.close();
		}

	}

}
