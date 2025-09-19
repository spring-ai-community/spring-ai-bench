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
package org.springaicommunity.bench.agents.hello;

import org.springaicommunity.bench.agents.verifier.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that the HelloWorldAgent created a file with exact content.
 * Simple verification using string equality and explicit UTF-8 reading.
 */
public class HelloWorldVerifier implements SuccessVerifier {

    private static final String EXPECTED = "Hello World!";

    @Override
    public VerificationResult verify(VerificationContext context) {
        List<Check> checks = new ArrayList<>();
        Path workspace = context.runRoot().resolve(context.workspaceRel());
        Path helloFile = workspace.resolve("hello.txt");

        // Check 1: File exists
        boolean exists = Files.exists(helloFile);
        checks.add(new Check("exists", exists, exists ? "ok" : "missing"));

        // Check 2: Content matches (if exists)
        boolean contentMatches = false;
        if (exists) {
            try {
                String content = Files.readString(helloFile, StandardCharsets.UTF_8);
                contentMatches = EXPECTED.equals(content);

                if (!contentMatches) {
                    // Show escaped content with visible newlines/spaces
                    String shown = content.replace("\n", "\\n").replace("\r", "\\r");
                    checks.add(new Check("content", false,
                        String.format("expected '%s' (len=%d) but got '%s' (len=%d)",
                            EXPECTED, EXPECTED.length(), shown, content.length())));
                } else {
                    checks.add(new Check("content", true, "ok"));
                }
            } catch (Exception e) {
                checks.add(new Check("content", false, "error reading file: " + e.getMessage()));
            }
        }

        boolean success = exists && contentMatches;
        return new VerificationResult(
            success,
            success ? "All checks passed" : "Verification failed",
            checks
        );
    }
}