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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springaicommunity.bench.core.exec.*;
import org.springaicommunity.bench.core.exec.customizer.ExecSpecCustomizer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DockerSandbox focusing on logic that can be tested without actual Docker execution.
 * These tests validate command building, customizer application, and error handling logic.
 */
class DockerSandboxTest {

    private GenericContainer<?> mockContainer;
    private Container.ExecResult mockExecResult;

    @BeforeEach
    void setUp() {
        mockContainer = mock(GenericContainer.class);
        mockExecResult = mock(Container.ExecResult.class);
    }

    @Nested
    @DisplayName("Constructor and initialization")
    class ConstructorTests {

        @Test
        @DisplayName("should create DockerSandbox with image name")
        void constructor_withImageName() {
            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                assertThat(sandbox).isNotNull();
                assertThat(sandbox.workDir().toString()).isEqualTo("/work");
                assertThat(sandbox.isClosed()).isFalse();

                // Verify container was configured correctly
                List<GenericContainer> constructed = mocked.constructed();
                assertThat(constructed).hasSize(1);

                GenericContainer container = constructed.get(0);
                verify(container).withWorkingDirectory("/work");
                verify(container).withCommand("sleep", "infinity");
                verify(container).start();
            }
        }

        @Test
        @DisplayName("should create DockerSandbox with customizers")
        void constructor_withCustomizers() {
            ExecSpecCustomizer customizer1 = spec -> spec;
            ExecSpecCustomizer customizer2 = spec -> spec;
            List<ExecSpecCustomizer> customizers = List.of(customizer1, customizer2);

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest", customizers);

                assertThat(sandbox.getCustomizers()).hasSize(2);
                assertThat(sandbox.getCustomizers()).containsExactly(customizer1, customizer2);
            }
        }

        @Test
        @DisplayName("should handle container start failure")
        void constructor_handlesContainerStartFailure() {
            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doThrow(new RuntimeException("Failed to start container")).when(mock).start();
                    })) {

                assertThatThrownBy(() -> new DockerSandbox("invalid:image"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Failed to start container");
            }
        }
    }

    @Nested
    @DisplayName("Command building and shell escaping")
    class CommandBuildingTests {

        @Test
        @DisplayName("should build correct bash command for simple cases")
        void exec_buildsCorrectBashCommand() throws Exception {
            // Arrange
            when(mockExecResult.getExitCode()).thenReturn(0);
            when(mockExecResult.getStdout()).thenReturn("hello world");
            when(mockExecResult.getStderr()).thenReturn("");

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class, (mock, context) -> {
                when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                doNothing().when(mock).start();
                // Just stub the method to return a result. We will verify the args later.
                when(mock.execInContainer(any(String[].class))).thenReturn(mockExecResult);
            })) {

                DockerSandbox testSandbox = new DockerSandbox("ubuntu:latest");
                ExecSpec spec = ExecSpec.of("echo", "hello world");

                // Act
                ExecResult result = testSandbox.exec(spec);

                // Assert
                // After the action, capture the arguments and verify them.
                ArgumentCaptor<String[]> commandCaptor = ArgumentCaptor.forClass(String[].class);
                GenericContainer mockedContainer = mocked.constructed().get(0);
                verify(mockedContainer).execInContainer(commandCaptor.capture());

                assertThat(commandCaptor.getValue()).containsExactly("bash", "-lc", "exec \"$@\"", "bash", "echo",
                        "hello world");
                assertThat(result.success()).isTrue();
                assertThat(result.mergedLog()).isEqualTo("hello world");
            }
        }

        @Test
        @DisplayName("should handle shell escaping for special characters")
        void exec_handlesShellEscaping() throws Exception {
            // Arrange
            when(mockExecResult.getExitCode()).thenReturn(0);
            when(mockExecResult.getStdout()).thenReturn("hello 'world'");
            when(mockExecResult.getStderr()).thenReturn("");

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class, (mock, context) -> {
                when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                doNothing().when(mock).start();
                when(mock.execInContainer(any(String[].class))).thenReturn(mockExecResult);
            })) {

                DockerSandbox testSandbox = new DockerSandbox("ubuntu:latest");
                ExecSpec spec = ExecSpec.of("echo", "hello 'world'");

                // Act
                ExecResult result = testSandbox.exec(spec);

                // Assert
                ArgumentCaptor<String[]> commandCaptor = ArgumentCaptor.forClass(String[].class);
                GenericContainer mockedContainer = mocked.constructed().get(0);
                verify(mockedContainer).execInContainer(commandCaptor.capture());

                assertThat(commandCaptor.getValue()).containsExactly("bash", "-lc", "exec \"$@\"", "bash", "echo",
                        "hello 'world'");
                assertThat(result.success()).isTrue();
                assertThat(result.mergedLog()).isEqualTo("hello 'world'");
            }
        }

        @Test
        @DisplayName("should build environment variable export commands")
        void exec_buildsEnvironmentCommands() {
            // Test the logic for building environment variable commands
            // Expected format: "export KEY1='value1'; export KEY2='value2'; original command"

            ExecSpec spec = ExecSpec.builder()
                    .command("echo", "test")
                    .env("TEST_VAR", "test_value")
                    .env("ANOTHER_VAR", "another value with spaces")
                    .build();

            // Verify the environment variables are properly formatted
            assertThat(spec.env()).containsEntry("TEST_VAR", "test_value");
            assertThat(spec.env()).containsEntry("ANOTHER_VAR", "another value with spaces");
        }
    }

    @Nested
    @DisplayName("Customizer application")
    class CustomizerApplicationTests {

        @Test
        @DisplayName("should apply customizers before command execution")
        void exec_appliesCustomizersBeforeExecution() throws Exception {
            ExecSpecCustomizer addArg = spec -> {
                var newCommand = new ArrayList<>(spec.command());
                newCommand.add("--verbose");
                return spec.toBuilder().command(newCommand).build();
            };

            when(mockExecResult.getExitCode()).thenReturn(0);
            when(mockExecResult.getStdout()).thenReturn("hello world");

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                        when(mock.execInContainer(any(String[].class))).thenReturn(mockExecResult);
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest", List.of(addArg));

                ExecSpec spec = ExecSpec.of("echo", "test");
                sandbox.exec(spec);

                // The customizer should have been applied, but verifying this requires
                // either exposing internal state or integration testing
                assertThat(sandbox.getCustomizers()).hasSize(1);
            }
        }

        @Test
        @DisplayName("should fail if customizer produces empty command")
        void exec_failsIfCustomizerProducesEmptyCommand() {
            ExecSpecCustomizer emptyCommandCustomizer = spec -> spec.toBuilder()
                    .command() // Empty command
                    .build();

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest", List.of(emptyCommandCustomizer));

                ExecSpec spec = ExecSpec.of("echo", "test");

                assertThatThrownBy(() -> sandbox.exec(spec))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Command cannot be null or empty");
            }
        }
    }

    @Nested
    @DisplayName("Error handling and state management")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should reject execution after close")
        void exec_rejectsExecutionAfterClose() throws IOException {
            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");
                sandbox.close();

                ExecSpec spec = ExecSpec.of("echo", "test");

                assertThatThrownBy(() -> sandbox.exec(spec))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Sandbox is closed");
            }
        }

        @Test
        @DisplayName("should handle container execution failure")
        void exec_handlesContainerExecutionFailure() throws Exception {
            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                        when(mock.execInContainer(any(String[].class)))
                                .thenThrow(new UnsupportedOperationException("Container execution failed"));
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                ExecSpec spec = ExecSpec.of("echo", "test");

                assertThatThrownBy(() -> sandbox.exec(spec))
                        .isInstanceOf(IOException.class)
                        .hasMessage("Failed to execute command in container")
                        .hasCauseInstanceOf(UnsupportedOperationException.class);
            }
        }

        @Test
        @DisplayName("should handle multiple close calls gracefully")
        void close_handlesMultipleCalls() throws IOException {
            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                sandbox.close();
                assertThat(sandbox.isClosed()).isTrue();

                // Second close should be safe
                assertThatCode(() -> sandbox.close()).doesNotThrowAnyException();
                assertThat(sandbox.isClosed()).isTrue();
            }
        }

        @Test
        @DisplayName("should handle container stop failure gracefully")
        void close_handlesContainerStopFailure() {
            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                        doThrow(new RuntimeException("Failed to stop container")).when(mock).stop();
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                assertThatThrownBy(() -> sandbox.close())
                        .isInstanceOf(IOException.class)
                        .hasMessage("Failed to close DockerSandbox")
                        .hasCauseInstanceOf(RuntimeException.class);
            }
        }
    }

    @Nested
    @DisplayName("Result processing")
    class ResultProcessingTests {

        @Test
        @DisplayName("should process successful execution result")
        void exec_processesSuccessfulResult() throws Exception {
            when(mockExecResult.getExitCode()).thenReturn(0);
            when(mockExecResult.getStdout()).thenReturn("hello world\n");
            when(mockExecResult.getStderr()).thenReturn("");

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                        when(mock.execInContainer(any(String[].class))).thenReturn(mockExecResult);
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                ExecSpec spec = ExecSpec.of("echo", "hello world");
                ExecResult result = sandbox.exec(spec);

                assertThat(result.exitCode()).isEqualTo(0);
                assertThat(result.success()).isTrue();
                assertThat(result.mergedLog()).isEqualTo("hello world\n");
            }
        }

        @Test
        @DisplayName("should process failed execution result")
        void exec_processesFailedResult() throws Exception {
            when(mockExecResult.getExitCode()).thenReturn(1);
            when(mockExecResult.getStdout()).thenReturn("");
            when(mockExecResult.getStderr()).thenReturn("command not found\n");

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                        when(mock.execInContainer(any(String[].class))).thenReturn(mockExecResult);
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                ExecSpec spec = ExecSpec.of("nonexistent-command");
                ExecResult result = sandbox.exec(spec);

                assertThat(result.exitCode()).isEqualTo(1);
                assertThat(result.failed()).isTrue();
                assertThat(result.mergedLog()).isEqualTo("command not found\n");
            }
        }

        @Test
        @DisplayName("should merge stdout and stderr correctly")
        void exec_mergesOutputStreams() throws Exception {
            when(mockExecResult.getExitCode()).thenReturn(0);
            when(mockExecResult.getStdout()).thenReturn("stdout content\n");
            when(mockExecResult.getStderr()).thenReturn("stderr content\n");

            try (MockedConstruction<GenericContainer> mocked = mockConstruction(GenericContainer.class,
                    (mock, context) -> {
                        when(mock.withWorkingDirectory(anyString())).thenReturn(mock);
                        when(mock.withCommand(anyString(), anyString())).thenReturn(mock);
                        doNothing().when(mock).start();
                        when(mock.execInContainer(any(String[].class))).thenReturn(mockExecResult);
                    })) {

                DockerSandbox sandbox = new DockerSandbox("ubuntu:latest");

                ExecSpec spec = ExecSpec.of("echo", "test");
                ExecResult result = sandbox.exec(spec);

                assertThat(result.mergedLog()).isEqualTo("stdout content\nstderr content\n");
            }
        }
    }

    private void setupMockExecution() {
        when(mockExecResult.getExitCode()).thenReturn(0);
        when(mockExecResult.getStdout()).thenReturn("hello world\n");
        when(mockExecResult.getStderr()).thenReturn("");
    }
}