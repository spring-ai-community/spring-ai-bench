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
package org.springaicommunity.bench.core.run;

import java.time.Duration;
import java.nio.file.Path;
import java.util.List;

import org.springaicommunity.bench.core.spec.AgentSpec;
import org.springaicommunity.bench.core.exec.ExecSpec;
import org.springaicommunity.bench.core.exec.ExecResult;
import org.springaicommunity.bench.core.exec.sandbox.LocalSandbox;

/**
 * Dummy agent runner that applies a simple fix to the Calculator sqrt bug.
 * Uses LocalSandbox for proper command execution.
 */
class DummyPatchRunner implements AgentRunner {

    DummyPatchRunner() { }

    @Override
    public AgentResult run(Path workspaceDir, AgentSpec spec, Duration timeout) throws Exception {
        long start = System.currentTimeMillis();

        try (LocalSandbox sandbox = LocalSandbox.builder()
                .workingDirectory(workspaceDir)
                .build()) {

            // Apply the fix using sed
            ExecSpec sedSpec = ExecSpec.builder()
                .command(List.of("sed", "-i",
                    "s/return Math.sqrt(number);/if (number < 0) { throw new IllegalArgumentException(\"Cannot calculate square root of negative number\"); } return Math.sqrt(number);/",
                    "src/main/java/org/springaicommunity/bench/example/calculator/Calculator.java"))
                .timeout(timeout)
                .build();

            ExecResult result = sandbox.exec(sedSpec);
            long duration = System.currentTimeMillis() - start;

            return new AgentResult(result.exitCode(), null, duration);
        }
    }
}
