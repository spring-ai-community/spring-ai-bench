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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class McpConfig {

	private final List<String> servers;

	private final Map<String, String> secrets;

	private final boolean pullOnDemand;

	@JsonCreator
	private McpConfig(@JsonProperty("servers") List<String> servers,
			@JsonProperty("secrets") Map<String, String> secrets, @JsonProperty("pullOnDemand") boolean pullOnDemand) {
		this.servers = List.copyOf(servers);
		this.secrets = Map.copyOf(secrets);
		this.pullOnDemand = pullOnDemand;
	}

	// Getters
	public List<String> servers() {
		return servers;
	}

	public Map<String, String> secrets() {
		return secrets;
	}

	public boolean pullOnDemand() {
		return pullOnDemand;
	}

	// Convenience factory methods
	public static McpConfig of(String... serverNames) {
		return builder().servers(serverNames).build();
	}

	public static McpConfig withSecrets(Map<String, String> secrets, String... serverNames) {
		return builder().servers(serverNames).secrets(secrets).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static class Builder {

		private List<String> servers = List.of();

		private Map<String, String> secrets = Map.of();

		private boolean pullOnDemand = true; // sensible default

		public Builder() {
		}

		private Builder(McpConfig config) {
			this.servers = config.servers;
			this.secrets = config.secrets;
			this.pullOnDemand = config.pullOnDemand;
		}

		public Builder server(String serverName) {
			var newServers = new java.util.ArrayList<>(this.servers);
			newServers.add(serverName);
			this.servers = List.copyOf(newServers);
			return this;
		}

		public Builder servers(String... serverNames) {
			this.servers = List.of(serverNames);
			return this;
		}

		public Builder servers(List<String> serverNames) {
			this.servers = List.copyOf(serverNames);
			return this;
		}

		public Builder secret(String key, String value) {
			var newSecrets = new HashMap<>(this.secrets);
			newSecrets.put(key, value);
			this.secrets = Map.copyOf(newSecrets);
			return this;
		}

		public Builder secrets(Map<String, String> secrets) {
			this.secrets = Map.copyOf(secrets);
			return this;
		}

		public Builder pullOnDemand(boolean pullOnDemand) {
			this.pullOnDemand = pullOnDemand;
			return this;
		}

		// Convenience methods for common server types
		public Builder withBrave(String apiKey) {
			return server("brave").secret("brave.api_key", apiKey);
		}

		public Builder withFilesystem() {
			return server("filesystem");
		}

		public Builder withGitHub(String token) {
			return server("github").secret("github.token", token);
		}

		public Builder withSlack(String token) {
			return server("slack").secret("slack.token", token);
		}

		public McpConfig build() {
			if (servers.isEmpty()) {
				throw new IllegalArgumentException("At least one server must be specified");
			}
			return new McpConfig(servers, secrets, pullOnDemand);
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		McpConfig mcpConfig = (McpConfig) o;
		return pullOnDemand == mcpConfig.pullOnDemand && Objects.equals(servers, mcpConfig.servers)
				&& Objects.equals(secrets, mcpConfig.secrets);
	}

	@Override
	public int hashCode() {
		return Objects.hash(servers, secrets, pullOnDemand);
	}

	@Override
	public String toString() {
		// Don't expose secrets in toString
		return "McpConfig{" + "servers=" + servers + ", secretCount=" + secrets.size() + ", pullOnDemand="
				+ pullOnDemand + '}';
	}

}
