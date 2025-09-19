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

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.bench.core.exec.ExecSpec;
import org.springaicommunity.bench.core.exec.McpConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ExecSpecCustomizer interface default methods.
 */
class ExecSpecCustomizerTest {

    private static final ExecSpec SAMPLE_SPEC = ExecSpec.builder()
            .command("echo", "hello")
            .env("TEST_VAR", "value")
            .timeout(Duration.ofSeconds(30))
            .build();

    @Nested
    @DisplayName("chain() method tests")
    class ChainTests {

        @Test
        @DisplayName("should apply single customizer")
        void chain_appliesSingleCustomizer() {
            ExecSpecCustomizer addArg = spec -> spec.toBuilder()
                    .command(spec.command().get(0), spec.command().get(1), "world")
                    .build();

            ExecSpecCustomizer chained = ExecSpecCustomizer.chain(addArg);
            ExecSpec result = chained.customize(SAMPLE_SPEC);

            assertThat(result.command()).containsExactly("echo", "hello", "world");
        }

        @Test
        @DisplayName("should apply multiple customizers in order")
        void chain_appliesMultipleCustomizersInOrder() {
            ExecSpecCustomizer addWorld = spec -> spec.toBuilder()
                    .command(spec.command().get(0), spec.command().get(1), "world")
                    .build();

            ExecSpecCustomizer addExclamation = spec -> {
                var cmd = new java.util.ArrayList<>(spec.command());
                cmd.add("!");
                return spec.toBuilder().command(cmd).build();
            };

            ExecSpecCustomizer chained = ExecSpecCustomizer.chain(addWorld, addExclamation);
            ExecSpec result = chained.customize(SAMPLE_SPEC);

            assertThat(result.command()).containsExactly("echo", "hello", "world", "!");
        }

        @Test
        @DisplayName("should handle empty customizer array")
        void chain_handlesEmptyArray() {
            ExecSpecCustomizer chained = ExecSpecCustomizer.chain();
            ExecSpec result = chained.customize(SAMPLE_SPEC);

            assertThat(result).isSameAs(SAMPLE_SPEC);
        }

        @Test
        @DisplayName("should reject null customizers with descriptive error")
        void chain_throwsOnNullCustomizer() {
            assertThatThrownBy(() -> {
                ExecSpecCustomizer.chain((ExecSpecCustomizer) null);
            }).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Customizer at index 0 cannot be null");
        }

        @Test
        @DisplayName("should reject null customizers in mixed array")
        void chain_throwsOnNullCustomizerInMixedArray() {
            ExecSpecCustomizer validCustomizer = ExecSpecCustomizer.identity();
            
            assertThatThrownBy(() -> {
                ExecSpecCustomizer.chain(validCustomizer, null, validCustomizer);
            }).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Customizer at index 1 cannot be null");
        }
    }

    @Nested
    @DisplayName("identity() method tests")
    class IdentityTests {

        @Test
        @DisplayName("should return same spec unchanged")
        void identity_returnsSameSpec() {
            ExecSpecCustomizer identity = ExecSpecCustomizer.identity();
            ExecSpec result = identity.customize(SAMPLE_SPEC);

            assertThat(result).isSameAs(SAMPLE_SPEC);
        }

        @Test
        @DisplayName("should work with complex specs")
        void identity_worksWithComplexSpecs() {
            ExecSpec complexSpec = ExecSpec.builder()
                    .command("complex", "command", "with", "args")
                    .env(Map.of("VAR1", "value1", "VAR2", "value2"))
                    .timeout(Duration.ofMinutes(5))
                    .mcp(McpConfig.builder()
                            .servers("brave", "filesystem")
                            .secret("api_key", "secret")
                            .build())
                    .build();

            ExecSpecCustomizer identity = ExecSpecCustomizer.identity();
            ExecSpec result = identity.customize(complexSpec);

            assertThat(result).isSameAs(complexSpec);
        }
    }

    @Nested
    @DisplayName("when() method tests")
    class WhenTests {

        @Test
        @DisplayName("should apply customizer when predicate is true")
        void when_appliesCustomizerWhenPredicateTrue() {
            ExecSpecCustomizer addVerbose = spec -> spec.toBuilder()
                    .command(spec.command().get(0), "-v", spec.command().get(1))
                    .build();

            ExecSpecCustomizer conditional = ExecSpecCustomizer.when(
                    spec -> spec.command().get(0).equals("echo"),
                    addVerbose
            );

            ExecSpec result = conditional.customize(SAMPLE_SPEC);

            assertThat(result.command()).containsExactly("echo", "-v", "hello");
        }

        @Test
        @DisplayName("should not apply customizer when predicate is false")
        void when_doesNotApplyCustomizerWhenPredicateFalse() {
            ExecSpecCustomizer addVerbose = spec -> spec.toBuilder()
                    .command(spec.command().get(0), "-v", spec.command().get(1))
                    .build();

            ExecSpecCustomizer conditional = ExecSpecCustomizer.when(
                    spec -> spec.command().get(0).equals("ls"),
                    addVerbose
            );

            ExecSpec result = conditional.customize(SAMPLE_SPEC);

            assertThat(result).isSameAs(SAMPLE_SPEC);
        }

        @Test
        @DisplayName("should handle complex predicates")
        void when_handlesComplexPredicates() {
            ExecSpecCustomizer addTimeout = spec -> spec.toBuilder()
                    .timeout(Duration.ofMinutes(10))
                    .build();

            ExecSpecCustomizer conditional = ExecSpecCustomizer.when(
                    spec -> spec.timeout() != null && spec.timeout().toSeconds() < 60,
                    addTimeout
            );

            ExecSpec result = conditional.customize(SAMPLE_SPEC);

            assertThat(result.timeout()).isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("should handle predicate exceptions gracefully")
        void when_handlePredicateExceptions() {
            ExecSpecCustomizer neverApplied = spec -> {
                throw new RuntimeException("Should not be called");
            };

            ExecSpecCustomizer conditional = ExecSpecCustomizer.when(
                    spec -> { throw new RuntimeException("Predicate failed"); },
                    neverApplied
            );

            assertThatThrownBy(() -> conditional.customize(SAMPLE_SPEC))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Predicate failed");
        }

        @Test
        @DisplayName("should throw for null predicate")
        void when_throwsOnNullPredicate() {
            assertThatThrownBy(() -> ExecSpecCustomizer.when(null, ExecSpecCustomizer.identity()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Predicate cannot be null");
        }

        @Test
        @DisplayName("should throw for null customizer")
        void when_throwsOnNullCustomizer() {
            assertThatThrownBy(() -> ExecSpecCustomizer.when(spec -> true, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Customizer cannot be null");
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        @DisplayName("should combine multiple default methods")
        void integration_combinesMultipleDefaultMethods() {
            ExecSpecCustomizer addWorld = spec -> spec.toBuilder()
                    .command(spec.command().get(0), spec.command().get(1), "world")
                    .build();

            ExecSpecCustomizer addVerbose = spec -> spec.toBuilder()
                    .command(spec.command().get(0), "-v", spec.command().get(1), spec.command().get(2))
                    .build();

            // Chain conditional customizers
            ExecSpecCustomizer combined = ExecSpecCustomizer.chain(
                    ExecSpecCustomizer.when(
                            spec -> spec.command().get(0).equals("echo"),
                            addWorld
                    ),
                    ExecSpecCustomizer.when(
                            spec -> spec.command().size() >= 3,
                            addVerbose
                    )
            );

            ExecSpec result = combined.customize(SAMPLE_SPEC);

            assertThat(result.command()).containsExactly("echo", "-v", "hello", "world");
        }
    }
}