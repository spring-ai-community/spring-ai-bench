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
package org.springaicommunity.bench.agents.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Service for managing workspace operations including cleanup and setup.
 */
@Service
public class WorkspaceService {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceService.class);

    /**
     * Cleans and recreates a workspace directory.
     * 
     * @param workspace the workspace directory to clean
     * @throws IOException if workspace operations fail
     */
    public void cleanWorkspace(Path workspace) throws IOException {
        logger.debug("Cleaning workspace: {}", workspace);
        
        if (Files.exists(workspace)) {
            logger.debug("Clearing existing workspace");
            Files.walk(workspace)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete path: {}", path, e);
                        // Log but don't fail - best effort cleanup
                    }
                });
        }
        
        Files.createDirectories(workspace);
        logger.debug("Created clean workspace: {}", workspace);
    }
}
