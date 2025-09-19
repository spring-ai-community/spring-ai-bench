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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for McpConfig functionality including builder pattern and convenience methods.
 */
class McpConfigTest {

    @Nested
    @DisplayName("Static factory methods")
    class StaticFactoryTests {

        @Test
        @DisplayName("should create config with single server using of()")
        void of_createsSingleServerConfig() {
            McpConfig config = McpConfig.of("brave");

            assertThat(config.servers()).containsExactly("brave");
            assertThat(config.secrets()).isEmpty();
            assertThat(config.pullOnDemand()).isTrue(); // default value
        }

        @Test
        @DisplayName("should create config with multiple servers using of()")
        void of_createsMultipleServerConfig() {
            McpConfig config = McpConfig.of("brave", "filesystem", "github");

            assertThat(config.servers()).containsExactly("brave", "filesystem", "github");
            assertThat(config.secrets()).isEmpty();
            assertThat(config.pullOnDemand()).isTrue();
        }

        @Test
        @DisplayName("should create config with secrets using withSecrets()")
        void withSecrets_createsConfigWithSecrets() {
            Map<String, String> secrets = Map.of("brave.api_key", "sk-test", "github.token", "ghp_test");

            McpConfig config = McpConfig.withSecrets(secrets, "brave", "github");

            assertThat(config.servers()).containsExactly("brave", "github");
            assertThat(config.secrets()).isEqualTo(secrets);
            assertThat(config.pullOnDemand()).isTrue();
        }

        @Test
        @DisplayName("should handle empty server list in of()")
        void of_handlesEmptyServerList() {
            assertThatThrownBy(() -> McpConfig.of())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one server must be specified");
        }
    }

    @Nested
    @DisplayName("Builder pattern")
    class BuilderTests {

        @Test
        @DisplayName("should build config with single server")
        void builder_buildsSingleServer() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .build();

            assertThat(config.servers()).containsExactly("brave");
            assertThat(config.secrets()).isEmpty();
            assertThat(config.pullOnDemand()).isTrue();
        }

        @Test
        @DisplayName("should build config with multiple servers using server()")
        void builder_buildsMultipleServersIndividually() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .server("filesystem")
                    .server("github")
                    .build();

            assertThat(config.servers()).containsExactly("brave", "filesystem", "github");
        }

        @Test
        @DisplayName("should build config with multiple servers using servers() varargs")
        void builder_buildsMultipleServersVarargs() {
            McpConfig config = McpConfig.builder()
                    .servers("brave", "filesystem", "github")
                    .build();

            assertThat(config.servers()).containsExactly("brave", "filesystem", "github");
        }

        @Test
        @DisplayName("should build config with servers list")
        void builder_buildsWithServersList() {
            List<String> serverList = List.of("brave", "filesystem");

            McpConfig config = McpConfig.builder()
                    .servers(serverList)
                    .build();

            assertThat(config.servers()).containsExactly("brave", "filesystem");
        }

        @Test
        @DisplayName("should build config with individual secrets")
        void builder_buildsWithIndividualSecrets() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .secret("brave.api_key", "sk-test")
                    .secret("debug", "true")
                    .build();

            assertThat(config.secrets())
                    .hasSize(2)
                    .containsEntry("brave.api_key", "sk-test")
                    .containsEntry("debug", "true");
        }

        @Test
        @DisplayName("should build config with secrets map")
        void builder_buildsWithSecretsMap() {
            Map<String, String> secrets = Map.of(
                    "brave.api_key", "sk-test",
                    "github.token", "ghp_test"
            );

            McpConfig config = McpConfig.builder()
                    .servers("brave", "github")
                    .secrets(secrets)
                    .build();

            assertThat(config.secrets()).isEqualTo(secrets);
        }

        @Test
        @DisplayName("should override pullOnDemand default")
        void builder_overridesPullOnDemand() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .pullOnDemand(false)
                    .build();

            assertThat(config.pullOnDemand()).isFalse();
        }

        @Test
        @DisplayName("should accumulate multiple server() calls")
        void builder_accumulatesServerCalls() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .server("filesystem")
                    .build();

            assertThat(config.servers()).containsExactly("brave", "filesystem");
        }

        @Test
        @DisplayName("should replace existing servers when servers() is called")
        void builder_serversMethodReplacesPrevious() {
            McpConfig config = McpConfig.builder()
                    .server("initial")
                    .servers("brave", "filesystem")
                    .server("additional")
                    .build();

            assertThat(config.servers()).containsExactly("brave", "filesystem", "additional");
        }

        @Test
        @DisplayName("should accumulate multiple secret() calls")
        void builder_accumulatesSecrets() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .secret("key1", "value1")
                    .secret("key2", "value2")
                    .build();

            assertThat(config.secrets())
                    .hasSize(2)
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("should replace existing secrets when secrets() is called")
        void builder_secretsMethodReplacesPrevious() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .secret("key1", "value1")
                    .secrets(Map.of("key2", "value2"))
                    .secret("key3", "value3")
                    .build();

            assertThat(config.secrets())
                    .hasSize(2)
                    .containsEntry("key2", "value2")
                    .containsEntry("key3", "value3")
                    .doesNotContainKey("key1");
        }

        @Test
        @DisplayName("should fail to build without servers")
        void builder_failsWithoutServers() {
            assertThatThrownBy(() -> McpConfig.builder().build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("At least one server must be specified");
        }
    }

    @Nested
    @DisplayName("Convenience builder methods")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("should configure Brave with API key")
        void withBrave_configuresBraveWithApiKey() {
            McpConfig config = McpConfig.builder()
                    .withBrave("sk-test-key")
                    .build();

            assertThat(config.servers()).contains("brave");
            assertThat(config.secrets()).containsEntry("brave.api_key", "sk-test-key");
        }

        @Test
        @DisplayName("should configure filesystem server")
        void withFilesystem_configuresFilesystemServer() {
            McpConfig config = McpConfig.builder()
                    .withFilesystem()
                    .build();

            assertThat(config.servers()).contains("filesystem");
            assertThat(config.secrets()).isEmpty();
        }

        @Test
        @DisplayName("should configure GitHub with token")
        void withGitHub_configuresGitHubWithToken() {
            McpConfig config = McpConfig.builder()
                    .withGitHub("ghp_test_token")
                    .build();

            assertThat(config.servers()).contains("github");
            assertThat(config.secrets()).containsEntry("github.token", "ghp_test_token");
        }

        @Test
        @DisplayName("should configure Slack with token")
        void withSlack_configuresSlackWithToken() {
            McpConfig config = McpConfig.builder()
                    .withSlack("xoxb-test-token")
                    .build();

            assertThat(config.servers()).contains("slack");
            assertThat(config.secrets()).containsEntry("slack.token", "xoxb-test-token");
        }

        @Test
        @DisplayName("should chain multiple convenience methods")
        void convenienceMethods_chainTogether() {
            McpConfig config = McpConfig.builder()
                    .withBrave("sk-test")
                    .withFilesystem()
                    .withGitHub("ghp-test")
                    .withSlack("xoxb-test")
                    .pullOnDemand(false)
                    .build();

            assertThat(config.servers())
                    .containsExactly("brave", "filesystem", "github", "slack");
            assertThat(config.secrets())
                    .hasSize(3)
                    .containsEntry("brave.api_key", "sk-test")
                    .containsEntry("github.token", "ghp-test")
                    .containsEntry("slack.token", "xoxb-test");
            assertThat(config.pullOnDemand()).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability and defensive copying")
    class ImmutabilityTests {

        @Test
        @DisplayName("should create defensive copies of servers list")
        void defensiveCopyServers() {
            List<String> originalServers = new java.util.ArrayList<>();
            originalServers.add("brave");

            McpConfig config = McpConfig.builder()
                    .servers(originalServers)
                    .build();

            // Modify original list
            originalServers.add("filesystem");

            // Config should not be affected
            assertThat(config.servers()).containsExactly("brave");
        }

        @Test
        @DisplayName("should create defensive copies of secrets map")
        void defensiveCopySecrets() {
            Map<String, String> originalSecrets = new java.util.HashMap<>();
            originalSecrets.put("key1", "value1");

            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .secrets(originalSecrets)
                    .build();

            // Modify original map
            originalSecrets.put("key2", "value2");

            // Config should not be affected
            assertThat(config.secrets()).hasSize(1).containsOnlyKeys("key1");
        }

        @Test
        @DisplayName("should return immutable servers list")
        void returnsImmutableServersList() {
            McpConfig config = McpConfig.of("brave", "filesystem");

            assertThatThrownBy(() -> config.servers().add("github"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return immutable secrets map")
        void returnsImmutableSecretsMap() {
            McpConfig config = McpConfig.builder()
                    .server("brave")
                    .secret("key", "value")
                    .build();

            assertThatThrownBy(() -> config.secrets().put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toString() behavior")
    class ToStringTests {

        @Test
        @DisplayName("should not expose secrets in toString")
        void toString_hidesSecrets() {
            McpConfig config = McpConfig.builder()
                    .withBrave("sk-secret-key")
                    .withGitHub("ghp-secret-token")
                    .build();

            String toString = config.toString();

            assertThat(toString)
                    .contains("servers=[brave, github]")
                    .contains("secretCount=2")
                    .contains("pullOnDemand=true")
                    .doesNotContain("sk-secret-key")
                    .doesNotContain("ghp-secret-token");
        }

        @Test
        @DisplayName("should show zero secret count when no secrets")
        void toString_showsZeroSecretCount() {
            McpConfig config = McpConfig.of("filesystem");

            String toString = config.toString();

            assertThat(toString).contains("secretCount=0");
        }
    }

    @Nested
    @DisplayName("Equals and hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("should be equal when all properties match")
        void equals_trueWhenAllPropertiesMatch() {
            McpConfig config1 = McpConfig.builder()
                    .servers("brave", "filesystem")
                    .secret("key", "value")
                    .pullOnDemand(false)
                    .build();

            McpConfig config2 = McpConfig.builder()
                    .servers("brave", "filesystem")
                    .secret("key", "value")
                    .pullOnDemand(false)
                    .build();

            assertThat(config1).isEqualTo(config2);
            assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when servers differ")
        void equals_falseWhenServersDiffer() {
            McpConfig config1 = McpConfig.of("brave");
            McpConfig config2 = McpConfig.of("filesystem");

            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("should not be equal when secrets differ")
        void equals_falseWhenSecretsDiffer() {
            McpConfig config1 = McpConfig.builder()
                    .server("brave")
                    .secret("key1", "value1")
                    .build();

            McpConfig config2 = McpConfig.builder()
                    .server("brave")
                    .secret("key2", "value2")
                    .build();

            assertThat(config1).isNotEqualTo(config2);
        }

        @Test
        @DisplayName("should not be equal when pullOnDemand differs")
        void equals_falseWhenPullOnDemandDiffers() {
            McpConfig config1 = McpConfig.builder()
                    .server("brave")
                    .pullOnDemand(true)
                    .build();

            McpConfig config2 = McpConfig.builder()
                    .server("brave")
                    .pullOnDemand(false)
                    .build();

            assertThat(config1).isNotEqualTo(config2);
        }
    }
}