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
package org.springaicommunity.bench.core.exec.customizer;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.bench.core.exec.ExecSpec;
import org.springaicommunity.bench.core.exec.McpConfig;

/**
 * Tests for ClaudeCliCustomizer functionality.
 *
 * <p>
 * This test class focuses on pure logic testing with no I/O dependencies, validating the
 * transformation behavior of the customizer.
 */
class ClaudeCliCustomizerTest {

	private ClaudeCliCustomizer customizer;

	@BeforeEach
	void setUp() {
		customizer = new ClaudeCliCustomizer();
	}

	@Nested
	@DisplayName("Claude CLI command detection")
	class CommandDetectionTests {

		@Test
		@DisplayName("should recognize claude-cli commands")
		void customize_recognizesClaudeCliCommand() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave", "filesystem"))
				.build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).containsExactly("claude-cli", "--run", "agent.py", "--tools=brave,filesystem");
		}

		@Test
		@DisplayName("should ignore non-claude-cli commands")
		void customize_ignoresNonClaudeCliCommands() {
			ExecSpec spec = ExecSpec.builder()
				.command("python", "script.py")
				.mcp(McpConfig.of("brave", "filesystem"))
				.build();

			ExecSpec result = customizer.customize(spec);

			// Should return the exact same instance (no modification)
			assertThat(result).isSameAs(spec);
			assertThat(result.command()).containsExactly("python", "script.py");
		}

		@Test
		@DisplayName("should be case-sensitive for command detection")
		void customize_isCaseSensitive() {
			ExecSpec spec = ExecSpec.builder()
				.command("CLAUDE-CLI", "--run", "agent.py")
				.mcp(McpConfig.of("brave"))
				.build();

			ExecSpec result = customizer.customize(spec);

			// Should not recognize uppercase variant
			assertThat(result).isSameAs(spec);
			assertThat(result.command()).containsExactly("CLAUDE-CLI", "--run", "agent.py");
		}

		@Test
		@DisplayName("should handle similar but different command names")
		void customize_handlesSimilarCommands() {
			List<String> similarCommands = List.of("claude", "claude-code", "cli-claude", "claude_cli", "claudecli");

			for (String command : similarCommands) {
				ExecSpec spec = ExecSpec.builder()
					.command(command, "--run", "agent.py")
					.mcp(McpConfig.of("brave"))
					.build();

				ExecSpec result = customizer.customize(spec);

				// None of these should be recognized as claude-cli
				assertThat(result).isSameAs(spec);
				assertThat(result.command().get(0)).isEqualTo(command);
			}
		}

	}

	@Nested
	@DisplayName("MCP tools handling")
	class McpToolsHandlingTests {

		@Test
		@DisplayName("should not customize when no MCP config")
		void customize_ignoresWhenNoMcpConfig() {
			ExecSpec spec = ExecSpec.builder().command("claude-cli", "--run", "agent.py").build(); // No
																									// MCP
																									// config

			ExecSpec result = customizer.customize(spec);

			assertThat(result).isSameAs(spec);
			assertThat(result.command()).containsExactly("claude-cli", "--run", "agent.py");
		}

		@Test
		@DisplayName("should add single tool correctly")
		void customize_addsSingleTool() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave"))
				.build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).containsExactly("claude-cli", "--run", "agent.py", "--tools=brave");
		}

		@Test
		@DisplayName("should add multiple tools comma-separated")
		void customize_addsMultipleTools() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave", "filesystem", "github"))
				.build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).containsExactly("claude-cli", "--run", "agent.py",
					"--tools=brave,filesystem,github");
		}

		@Test
		@DisplayName("should handle tools with special characters")
		void customize_handlesSpecialCharactersInTools() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.builder().servers("tool-with-dashes", "tool_with_underscores", "tool.with.dots").build())
				.build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).containsExactly("claude-cli", "--run", "agent.py",
					"--tools=tool-with-dashes,tool_with_underscores,tool.with.dots");
		}

		@Test
		@DisplayName("should preserve tool order from MCP config")
		void customize_preservesToolOrder() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("zulu", "alpha", "bravo"))
				.build();

			ExecSpec result = customizer.customize(spec);

			String toolsFlag = result.command().get(result.command().size() - 1);
			assertThat(toolsFlag).isEqualTo("--tools=zulu,alpha,bravo");
		}

	}

	@Nested
	@DisplayName("ExecSpec preservation and immutability")
	class ExecSpecPreservationTests {

		@Test
		@DisplayName("should preserve all other ExecSpec properties")
		void customize_preservesAllProperties() {
			Map<String, String> env = Map.of("API_KEY", "secret", "DEBUG", "true");
			Duration timeout = Duration.ofMinutes(5);
			McpConfig mcp = McpConfig.builder()
				.servers("brave", "filesystem")
				.secret("brave.api_key", "sk-test")
				.pullOnDemand(false)
				.build();

			ExecSpec original = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.env(env)
				.timeout(timeout)
				.mcp(mcp)
				.build();

			ExecSpec result = customizer.customize(original);

			// All properties except command should be identical
			assertThat(result.env()).isEqualTo(env);
			assertThat(result.timeout()).isEqualTo(timeout);
			assertThat(result.mcp()).isEqualTo(mcp);
		}

		@Test
		@DisplayName("should create new ExecSpec instance when customizing")
		void customize_createsNewInstance() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave"))
				.build();

			ExecSpec result = customizer.customize(spec);

			// Should be a different instance
			assertThat(result).isNotSameAs(spec);

			// But original should be unchanged
			assertThat(spec.command()).containsExactly("claude-cli", "--run", "agent.py");
			assertThat(result.command()).containsExactly("claude-cli", "--run", "agent.py", "--tools=brave");
		}

		@Test
		@DisplayName("should fail when --tools flag already exists")
		void customize_failsOnExistingToolsFlag() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py", "--tools=existing")
				.mcp(McpConfig.of("brave", "filesystem"))
				.build();

			assertThatThrownBy(() -> customizer.customize(spec)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already contains a '--tools' flag");
		}

		@Test
		@DisplayName("should not modify original spec")
		void customize_doesNotModifyOriginal() {
			ExecSpec original = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave"))
				.build();

			List<String> originalCommand = original.command();

			customizer.customize(original);

			// Original should be completely unchanged
			assertThat(original.command()).isEqualTo(originalCommand);
			assertThat(original.command()).hasSize(3);
		}

	}

	@Nested
	@DisplayName("getToolsFlag() utility method")
	class GetToolsFlagTests {

		@Test
		@DisplayName("should return correct tools flag for valid claude-cli command")
		void getToolsFlag_returnsCorrectFlag() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave", "filesystem"))
				.build();

			String toolsFlag = customizer.getToolsFlag(spec);

			assertThat(toolsFlag).isEqualTo("--tools=brave,filesystem");
		}

		@Test
		@DisplayName("should return null for non-claude-cli command")
		void getToolsFlag_returnsNullForNonClaudeCommand() {
			ExecSpec spec = ExecSpec.builder().command("python", "script.py").mcp(McpConfig.of("brave")).build();

			String toolsFlag = customizer.getToolsFlag(spec);

			assertThat(toolsFlag).isNull();
		}

		@Test
		@DisplayName("should return null when no MCP config")
		void getToolsFlag_returnsNullWhenNoMcp() {
			ExecSpec spec = ExecSpec.builder().command("claude-cli", "--run", "agent.py").build();

			String toolsFlag = customizer.getToolsFlag(spec);

			assertThat(toolsFlag).isNull();
		}

		@Test
		@DisplayName("should be consistent with customize() behavior")
		void getToolsFlag_consistentWithCustomize() {
			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of("brave", "filesystem"))
				.build();

			String toolsFlag = customizer.getToolsFlag(spec);
			ExecSpec customized = customizer.customize(spec);

			// The tools flag should match what was added to the command
			assertThat(customized.command()).endsWith(toolsFlag);
		}

	}

	@Nested
	@DisplayName("Edge cases and robustness")
	class EdgeCasesTests {

		@Test
		@DisplayName("should handle command with only claude-cli")
		void customize_handlesMinimalCommand() {
			ExecSpec spec = ExecSpec.builder().command("claude-cli").mcp(McpConfig.of("brave")).build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).containsExactly("claude-cli", "--tools=brave");
		}

		@Test
		@DisplayName("should handle very long command list")
		void customize_handlesLongCommandList() {
			List<String> longCommand = List.of("claude-cli", "--run", "agent.py", "--verbose", "--debug", "--config",
					"config.json", "--output", "results.txt", "--parallel", "4");

			ExecSpec spec = ExecSpec.builder().command(longCommand).mcp(McpConfig.of("brave", "filesystem")).build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).startsWith("claude-cli", "--run", "agent.py", "--verbose", "--debug")
				.endsWith("--tools=brave,filesystem");
			assertThat(result.command()).hasSize(longCommand.size() + 1);
		}

		@Test
		@DisplayName("should be thread-safe")
		void customize_isThreadSafe() throws InterruptedException {
			ExecSpec spec1 = ExecSpec.builder()
				.command("claude-cli", "--run", "agent1.py")
				.mcp(McpConfig.of("brave"))
				.build();

			ExecSpec spec2 = ExecSpec.builder()
				.command("claude-cli", "--run", "agent2.py")
				.mcp(McpConfig.of("filesystem"))
				.build();

			// Simulate concurrent access
			Thread thread1 = new Thread(() -> customizer.customize(spec1));
			Thread thread2 = new Thread(() -> customizer.customize(spec2));

			thread1.start();
			thread2.start();

			thread1.join();
			thread2.join();

			// Verify both work correctly (no shared state corruption)
			ExecSpec result1 = customizer.customize(spec1);
			ExecSpec result2 = customizer.customize(spec2);

			assertThat(result1.command()).containsExactly("claude-cli", "--run", "agent1.py", "--tools=brave");
			assertThat(result2.command()).containsExactly("claude-cli", "--run", "agent2.py", "--tools=filesystem");
		}

		@Test
		@DisplayName("should handle null MCP gracefully")
		void customize_handlesNullMcp() {
			ExecSpec spec = ExecSpec.builder().command("claude-cli", "--run", "agent.py").mcp(null).build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result).isSameAs(spec);
		}

		@Test
		@DisplayName("should handle very large number of tools")
		void customize_handlesLargeNumberOfTools() {
			String[] manyTools = new String[100];
			for (int i = 0; i < 100; i++) {
				manyTools[i] = "tool" + i;
			}

			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--run", "agent.py")
				.mcp(McpConfig.of(manyTools))
				.build();

			ExecSpec result = customizer.customize(spec);

			String toolsFlag = result.command().get(result.command().size() - 1);
			assertThat(toolsFlag).startsWith("--tools=tool0,tool1,tool2");
			assertThat(toolsFlag).endsWith("tool97,tool98,tool99");

			// Verify it contains all 100 tools
			String toolsList = toolsFlag.substring("--tools=".length());
			assertThat(toolsList.split(",")).hasSize(100);
		}

	}

	@Nested
	@DisplayName("Integration with ExecSpec patterns")
	class IntegrationTests {

		@Test
		@DisplayName("should work correctly with ExecSpec.of() factory")
		void customize_worksWithStaticFactory() {
			ExecSpec spec = ExecSpec.of("claude-cli", "--run", "agent.py");
			// Note: ExecSpec.of() doesn't set MCP, so this should be a no-op

			ExecSpec result = customizer.customize(spec);

			assertThat(result).isSameAs(spec); // No MCP, so no changes
		}

		@Test
		@DisplayName("should work with complex McpConfig")
		void customize_worksWithComplexMcpConfig() {
			McpConfig complexMcp = McpConfig.builder()
				.withBrave("sk-test")
				.withFilesystem()
				.withGitHub("ghp-test")
				.pullOnDemand(false)
				.build();

			ExecSpec spec = ExecSpec.builder()
				.command("claude-cli", "--interactive")
				.env("DEBUG", "true")
				.timeout(Duration.ofMinutes(30))
				.mcp(complexMcp)
				.build();

			ExecSpec result = customizer.customize(spec);

			assertThat(result.command()).containsExactly("claude-cli", "--interactive",
					"--tools=brave,filesystem,github");

			// Verify all other properties preserved
			assertThat(result.env()).containsEntry("DEBUG", "true");
			assertThat(result.timeout()).isEqualTo(Duration.ofMinutes(30));
			assertThat(result.mcp()).isEqualTo(complexMcp);
		}

	}

}
