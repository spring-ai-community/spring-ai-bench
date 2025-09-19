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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ExecSpec functionality including builder pattern and validation.
 */
class ExecSpecTest {

    @Nested
    @DisplayName("Static factory methods")
    class StaticFactoryTests {

        @Test
        @DisplayName("should create simple spec using of()")
        void of_createsSimpleSpec() {
            ExecSpec spec = ExecSpec.of("git", "status");

            assertThat(spec.command()).containsExactly("git", "status");
            assertThat(spec.env()).isEmpty();
            assertThat(spec.timeout()).isNull();
            assertThat(spec.mcp()).isNull();
        }

        @Test
        @DisplayName("should create spec with single command using of()")
        void of_createsSingleCommandSpec() {
            ExecSpec spec = ExecSpec.of("pwd");

            assertThat(spec.command()).containsExactly("pwd");
            assertThat(spec.env()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty command in of()")
        void of_handlesEmptyCommand() {
            assertThatThrownBy(() -> ExecSpec.of())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Command cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Builder pattern")
    class BuilderTests {

        @Test
        @DisplayName("should build spec with command varargs")
        void builder_buildsWithCommandVarargs() {
            ExecSpec spec = ExecSpec.builder()
                    .command("git", "clone", "https://github.com/user/repo")
                    .build();

            assertThat(spec.command()).containsExactly("git", "clone", "https://github.com/user/repo");
        }

        @Test
        @DisplayName("should build spec with command list")
        void builder_buildsWithCommandList() {
            List<String> command = List.of("mvn", "clean", "test");

            ExecSpec spec = ExecSpec.builder()
                    .command(command)
                    .build();

            assertThat(spec.command()).containsExactly("mvn", "clean", "test");
        }

        @Test
        @DisplayName("should build spec with individual environment variables")
        void builder_buildsWithIndividualEnvVars() {
            ExecSpec spec = ExecSpec.builder()
                    .command("echo", "test")
                    .env("JAVA_HOME", "/usr/lib/jvm/java-17")
                    .env("PATH", "/usr/bin:/bin")
                    .build();

            assertThat(spec.env())
                    .hasSize(2)
                    .containsEntry("JAVA_HOME", "/usr/lib/jvm/java-17")
                    .containsEntry("PATH", "/usr/bin:/bin");
        }

        @Test
        @DisplayName("should build spec with environment map")
        void builder_buildsWithEnvMap() {
            Map<String, String> env = Map.of(
                    "API_KEY", "secret",
                    "DEBUG", "true"
            );

            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env(env)
                    .build();

            assertThat(spec.env()).isEqualTo(env);
        }

        @Test
        @DisplayName("should build spec with timeout")
        void builder_buildsWithTimeout() {
            Duration timeout = Duration.ofMinutes(5);

            ExecSpec spec = ExecSpec.builder()
                    .command("long-running-task")
                    .timeout(timeout)
                    .build();

            assertThat(spec.timeout()).isEqualTo(timeout);
        }

        @Test
        @DisplayName("should build spec with MCP config")
        void builder_buildsWithMcpConfig() {
            McpConfig mcp = McpConfig.of("brave", "filesystem");

            ExecSpec spec = ExecSpec.builder()
                    .command("claude-cli", "--run", "agent.py")
                    .mcp(mcp)
                    .build();

            assertThat(spec.mcp()).isEqualTo(mcp);
        }

        @Test
        @DisplayName("should build complete spec with all properties")
        void builder_buildsCompleteSpec() {
            Duration timeout = Duration.ofMinutes(10);
            Map<String, String> env = Map.of("API_KEY", "secret", "DEBUG", "true");
            McpConfig mcp = McpConfig.builder()
                    .withBrave("sk-test")
                    .withFilesystem()
                    .build();

            ExecSpec spec = ExecSpec.builder()
                    .command("claude-cli", "--run", "complex-agent.py")
                    .env(env)
                    .timeout(timeout)
                    .mcp(mcp)
                    .build();

            assertThat(spec.command()).containsExactly("claude-cli", "--run", "complex-agent.py");
            assertThat(spec.env()).isEqualTo(env);
            assertThat(spec.timeout()).isEqualTo(timeout);
            assertThat(spec.mcp()).isEqualTo(mcp);
        }

        @Test
        @DisplayName("should accumulate multiple env() calls")
        void builder_accumulatesEnvCalls() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env("KEY1", "value1")
                    .env("KEY2", "value2")
                    .env(Map.of("KEY3", "value3")) // This might replace existing
                    .env("KEY4", "value4")
                    .build();

            assertThat(spec.env())
                    .containsEntry("KEY3", "value3")
                    .containsEntry("KEY4", "value4");
        }

        @Test
        @DisplayName("should replace existing environment when env() map method is called")
        void builder_envMapMethodReplacesPrevious() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env("initial_key", "initial_value") // Should be replaced
                    .env(Map.of("KEY1", "value1", "KEY2", "value2")) // Replaces previous
                    .env("final_key", "final_value") // Accumulates after replacement
                    .build();

            assertThat(spec.env())
                    .hasSize(3)
                    .containsEntry("KEY1", "value1")
                    .containsEntry("KEY2", "value2")
                    .containsEntry("final_key", "final_value")
                    .doesNotContainKey("initial_key");
        }

        @Test
        @DisplayName("should fail to build without command")
        void builder_failsWithoutCommand() {
            assertThatThrownBy(() -> ExecSpec.builder().build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Command cannot be null or empty");
        }

        @Test
        @DisplayName("should fail to build with empty command")
        void builder_failsWithEmptyCommand() {
            assertThatThrownBy(() -> ExecSpec.builder().command().build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Command cannot be null or empty");
        }

        @Test
        @DisplayName("should fail to build with null command list")
        void builder_failsWithNullCommandList() {
            assertThatThrownBy(() -> ExecSpec.builder().command((List<String>) null).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Immutability and defensive copying")
    class ImmutabilityTests {

        @Test
        @DisplayName("should create defensive copy of command list")
        void defensiveCopyCommand() {
            List<String> originalCommand = new java.util.ArrayList<>();
            originalCommand.add("git");
            originalCommand.add("status");

            ExecSpec spec = ExecSpec.builder()
                    .command(originalCommand)
                    .build();

            // Modify original list
            originalCommand.add("--verbose");

            // Spec should not be affected
            assertThat(spec.command()).containsExactly("git", "status");
        }

        @Test
        @DisplayName("should create defensive copy of environment map")
        void defensiveCopyEnvironment() {
            Map<String, String> originalEnv = new java.util.HashMap<>();
            originalEnv.put("KEY1", "value1");

            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env(originalEnv)
                    .build();

            // Modify original map
            originalEnv.put("KEY2", "value2");

            // Spec should not be affected
            assertThat(spec.env()).hasSize(1).containsOnlyKeys("KEY1");
        }

        @Test
        @DisplayName("should return immutable command list")
        void returnsImmutableCommandList() {
            ExecSpec spec = ExecSpec.of("git", "status");

            assertThatThrownBy(() -> spec.command().add("--verbose"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable environment map")
        void returnsImmutableEnvironmentMap() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env("KEY", "value")
                    .build();

            assertThatThrownBy(() -> spec.env().put("NEW_KEY", "new_value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Edge cases and validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle command with spaces and special characters")
        void handlesSpecialCharactersInCommand() {
            ExecSpec spec = ExecSpec.builder()
                    .command("echo", "hello world", "with spaces", "and-dashes", "under_scores")
                    .build();

            assertThat(spec.command())
                    .containsExactly("echo", "hello world", "with spaces", "and-dashes", "under_scores");
        }

        @Test
        @DisplayName("should handle environment values with special characters")
        void handlesSpecialCharactersInEnv() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env("PATH", "/usr/bin:/bin:/usr/local/bin")
                    .env("COMPLEX_VAR", "value with spaces and symbols !@#$%")
                    .env("JSON_CONFIG", "{\"key\": \"value\"}")
                    .build();

            assertThat(spec.env())
                    .containsEntry("PATH", "/usr/bin:/bin:/usr/local/bin")
                    .containsEntry("COMPLEX_VAR", "value with spaces and symbols !@#$%")
                    .containsEntry("JSON_CONFIG", "{\"key\": \"value\"}");
        }

        @Test
        @DisplayName("should handle very long command lists")
        void handlesLongCommandLists() {
            String[] longCommand = new String[100];
            for (int i = 0; i < 100; i++) {
                longCommand[i] = "arg" + i;
            }

            ExecSpec spec = ExecSpec.builder()
                    .command(longCommand)
                    .build();

            assertThat(spec.command()).hasSize(100);
            assertThat(spec.command().get(0)).isEqualTo("arg0");
            assertThat(spec.command().get(99)).isEqualTo("arg99");
        }

        @Test
        @DisplayName("should handle zero timeout")
        void handlesZeroTimeout() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .timeout(Duration.ZERO)
                    .build();

            assertThat(spec.timeout()).isEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("should handle very large timeout")
        void handlesLargeTimeout() {
            Duration largeTimeout = Duration.ofDays(365);

            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .timeout(largeTimeout)
                    .build();

            assertThat(spec.timeout()).isEqualTo(largeTimeout);
        }

        @Test
        @DisplayName("should handle empty environment gracefully")
        void handlesEmptyEnvironment() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env(Map.of())
                    .build();

            assertThat(spec.env()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Equals and hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("should be equal when all properties match")
        void equals_trueWhenAllPropertiesMatch() {
            Duration timeout = Duration.ofMinutes(5);
            Map<String, String> env = Map.of("KEY", "value");
            McpConfig mcp = McpConfig.of("brave");

            ExecSpec spec1 = ExecSpec.builder()
                    .command("test", "command")
                    .env(env)
                    .timeout(timeout)
                    .mcp(mcp)
                    .build();

            ExecSpec spec2 = ExecSpec.builder()
                    .command("test", "command")
                    .env(env)
                    .timeout(timeout)
                    .mcp(mcp)
                    .build();

            assertThat(spec1).isEqualTo(spec2);
            assertThat(spec1.hashCode()).isEqualTo(spec2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when commands differ")
        void equals_falseWhenCommandsDiffer() {
            ExecSpec spec1 = ExecSpec.of("git", "status");
            ExecSpec spec2 = ExecSpec.of("git", "log");

            assertThat(spec1).isNotEqualTo(spec2);
        }

        @Test
        @DisplayName("should not be equal when environment differs")
        void equals_falseWhenEnvironmentDiffers() {
            ExecSpec spec1 = ExecSpec.builder()
                    .command("test")
                    .env("KEY1", "value1")
                    .build();

            ExecSpec spec2 = ExecSpec.builder()
                    .command("test")
                    .env("KEY2", "value2")
                    .build();

            assertThat(spec1).isNotEqualTo(spec2);
        }

        @Test
        @DisplayName("should not be equal when timeout differs")
        void equals_falseWhenTimeoutDiffers() {
            ExecSpec spec1 = ExecSpec.builder()
                    .command("test")
                    .timeout(Duration.ofMinutes(5))
                    .build();

            ExecSpec spec2 = ExecSpec.builder()
                    .command("test")
                    .timeout(Duration.ofMinutes(10))
                    .build();

            assertThat(spec1).isNotEqualTo(spec2);
        }

        @Test
        @DisplayName("should not be equal when MCP config differs")
        void equals_falseWhenMcpDiffers() {
            ExecSpec spec1 = ExecSpec.builder()
                    .command("test")
                    .mcp(McpConfig.of("brave"))
                    .build();

            ExecSpec spec2 = ExecSpec.builder()
                    .command("test")
                    .mcp(McpConfig.of("filesystem"))
                    .build();

            assertThat(spec1).isNotEqualTo(spec2);
        }

        @Test
        @DisplayName("should handle null properties in equals")
        void equals_handlesNullProperties() {
            ExecSpec spec1 = ExecSpec.builder()
                    .command("test")
                    .build();

            ExecSpec spec2 = ExecSpec.builder()
                    .command("test")
                    .timeout(null)
                    .mcp(null)
                    .build();

            assertThat(spec1).isEqualTo(spec2);
        }
    }

    @Nested
    @DisplayName("toString() behavior")
    class ToStringTests {

        @Test
        @DisplayName("should include all properties in toString")
        void toString_includesAllProperties() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test", "command")
                    .env("KEY", "value")
                    .timeout(Duration.ofMinutes(5))
                    .mcp(McpConfig.of("brave"))
                    .build();

            String toString = spec.toString();

            assertThat(toString)
                    .contains("command=[test, command]")
                    .contains("env={KEY=value}")
                    .contains("timeout=PT5M")
                    .contains("mcp=McpConfig");
        }

        @Test
        @DisplayName("should handle null properties in toString")
        void toString_handlesNullProperties() {
            ExecSpec spec = ExecSpec.of("test");

            String toString = spec.toString();

            assertThat(toString)
                    .contains("command=[test]")
                    .contains("env={}")
                    .contains("timeout=null")
                    .contains("mcp=null");
        }
    }

    @Nested
    @DisplayName("Builder reuse and isolation")
    class BuilderIsolationTests {

        @Test
        @DisplayName("should create independent specs from same builder")
        void builder_createsIndependentSpecs() {
            ExecSpec.Builder builder = ExecSpec.builder()
                    .command("base", "command")
                    .env("SHARED", "value");

            ExecSpec spec1 = builder.env("SPEC1", "value1").build();
            ExecSpec spec2 = builder.env("SPEC2", "value2").build();

            // Both specs should have the shared environment but be independent
            assertThat(spec1.env()).containsKey("SHARED");
            assertThat(spec2.env()).containsKey("SHARED");

            // But they should be different objects
            assertThat(spec1).isNotEqualTo(spec2);
        }

        @Test
        @DisplayName("should allow builder method chaining")
        void builder_allowsMethodChaining() {
            ExecSpec spec = ExecSpec.builder()
                    .command("test")
                    .env("KEY1", "value1")
                    .env("KEY2", "value2")
                    .timeout(Duration.ofMinutes(5))
                    .mcp(McpConfig.of("brave"))
                    .build();

            assertThat(spec.command()).containsExactly("test");
            assertThat(spec.env()).hasSize(2);
            assertThat(spec.timeout()).isEqualTo(Duration.ofMinutes(5));
            assertThat(spec.mcp()).isNotNull();
        }
    }
}