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

import java.io.IOException;
import java.nio.file.Path;

import org.springaicommunity.bench.core.exec.ExecResult;
import org.springaicommunity.bench.core.exec.ExecSpec;
import org.springaicommunity.bench.core.exec.TimeoutException;

public interface Sandbox extends AutoCloseable {
    ExecResult exec(ExecSpec spec)
            throws IOException, InterruptedException, TimeoutException;
    Path workDir();
    boolean isClosed();
    @Override void close() throws IOException;
}