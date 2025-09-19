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

import java.nio.file.Path;
import java.time.Duration;
import org.springaicommunity.bench.core.exec.ExecResult;
import org.springaicommunity.bench.core.exec.ExecSpec;
import org.springaicommunity.bench.core.exec.sandbox.LocalSandbox;
import org.springaicommunity.bench.core.spec.SuccessSpec;

/**
 * Verifies benchmark success by running the specified command. Uses LocalSandbox for
 * proper command execution.
 */
class SuccessVerifier {

	SuccessVerifier() {
	}

	boolean verify(Path workspaceDir, SuccessSpec spec, Duration timeout) throws Exception {
		try (LocalSandbox sandbox = LocalSandbox.builder().workingDirectory(workspaceDir).build()) {

			// Execute the command string directly - LocalSandbox will handle platform
			// details
			ExecSpec execSpec = ExecSpec.builder().shellCommand(spec.cmd()).timeout(timeout).build();

			ExecResult result = sandbox.exec(execSpec);
			return result.exitCode() == 0;
		}
	}

}
