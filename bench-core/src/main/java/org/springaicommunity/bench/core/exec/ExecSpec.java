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

import org.springframework.lang.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExecSpec {
    private final List<String> command;
    private final Map<String, String> env;
    private final Duration timeout;
    private final McpConfig mcp;

    @JsonCreator
    private ExecSpec(
            @JsonProperty("command") @NonNull List<String> command,
            @JsonProperty("env") @NonNull Map<String, String> env,
            @JsonProperty("timeout") Duration timeout,
            @JsonProperty("mcp") McpConfig mcp) {
        this.command = List.copyOf(command);
        this.env = Map.copyOf(env);
        this.timeout = timeout;
        this.mcp = mcp;
    }

    // Getters
    public List<String> command() { return command; }
    public Map<String, String> env() { return env; }
    public Duration timeout() { return timeout; }
    public McpConfig mcp() { return mcp; }

    // Convenience factory for simple cases
    public static ExecSpec of(String... cmd) {
        return builder().command(cmd).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private List<String> command;
        private Map<String, String> env = Map.of();
        private Duration timeout;
        private McpConfig mcp;

        public Builder() { }

        private Builder(ExecSpec spec) {
            this.command = spec.command;
            this.env = spec.env;
            this.timeout = spec.timeout;
            this.mcp = spec.mcp;
        }

        public Builder command(String... cmd) {
            if (cmd == null) {
                throw new IllegalArgumentException("Command varargs cannot be null");
            }
            this.command = List.of(cmd);
            return this;
        }

        public Builder command(List<String> cmd) {
            if (cmd == null) {
                throw new IllegalArgumentException("Command list cannot be null");
            }
            this.command = List.copyOf(cmd);
            return this;
        }

        /**
         * Set a shell command to be executed. The LocalSandbox will automatically
         * wrap this in the appropriate shell for the platform (bash -c on Unix, cmd /c on Windows).
         */
        public Builder shellCommand(String shellCmd) {
            if (shellCmd == null) {
                throw new IllegalArgumentException("Shell command cannot be null");
            }
            // Mark this as a shell command for LocalSandbox to process
            this.command = List.of("__SHELL_COMMAND__", shellCmd);
            return this;
        }

        public Builder env(String key, String value) {
            var newEnv = new HashMap<>(this.env);
            newEnv.put(key, value);
            this.env = Map.copyOf(newEnv);
            return this;
        }

        public Builder env(Map<String, String> envMap) {
            this.env = Map.copyOf(envMap);
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder mcp(McpConfig mcp) {
            this.mcp = mcp;
            return this;
        }

        public ExecSpec build() {
            if (command == null || command.isEmpty()) {
                throw new IllegalArgumentException("Command cannot be null or empty");
            }
            return new ExecSpec(command, env, timeout, mcp);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecSpec execSpec = (ExecSpec) o;
        return Objects.equals(command, execSpec.command) &&
                Objects.equals(env, execSpec.env) &&
                Objects.equals(timeout, execSpec.timeout) &&
                Objects.equals(mcp, execSpec.mcp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, env, timeout, mcp);
    }

    @Override
    public String toString() {
        return "ExecSpec{" +
                "command=" + command +
                ", env=" + env +
                ", timeout=" + timeout +
                ", mcp=" + mcp +
                '}';
    }
}
