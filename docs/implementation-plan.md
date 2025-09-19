# Plan: Real Agents Integration - Spring AI Bench + Spring AI Agents (FINAL v2)

## Overview
Integrate real agents (Claude Code and Gemini) from the Spring AI Agents project into Spring AI Bench, enabling benchmarking of actual AI agent implementations.

## Phase 1: Claude Code Agent Support

### 1.1 Dependencies
Add to `bench-agents/pom.xml`:
```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>spring-ai-claude-code</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>spring-ai-agent-client</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<!-- zt-exec is already included via spring-ai-agents dependencies -->
```

### 1.2 Implementation
Create `ClaudeCodeAgentRunner.java`:
- Wraps `ClaudeCodeAgentModel` with `AgentModelAdapter`
- Uses existing `HelloWorldVerifier` unchanged
- Maps `yolo=true` to `--dangerously-skip-permissions` for non-interactive mode
- **Uses zt-exec `ProcessExecutor.directory(workspace)`** to run CLI in Bench workspace
- Resolves CLI path via Spring AI Agents CLI discovery utilities
- Captures CLI version via `claude --version` using zt-exec with 3s timeout
- Adds comprehensive metadata under `provenance.agent`
- Passes explicit goal: "Create a file named hello.txt in the current working directory with EXACT contents: Hello World!"
- Logs environment status without values (e.g., "ANTHROPIC_API_KEY=present")

Example CLI execution:
```java
Path cliPath = cliDiscovery.require("claude");
new ProcessExecutor()
    .directory(workspace.toFile())
    .command(cliPath.toString(), "--dangerously-skip-permissions", /* ... */)
    .redirectErrorStream(true)
    .timeout(2, TimeUnit.MINUTES)
    .exitValue(0)
    .execute();
```

### 1.3 Auto-Configuration
Create `AgentProviderConfig.java`:
```java
@Configuration
public class AgentProviderConfig {
    @Bean
    @ConditionalOnClass(ClaudeCodeAgentModel.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    @ConditionalOnProperty(name="spring.ai.bench.agent.provider", havingValue="claude-code")
    public AgentRunner claudeRunner(ClaudeCodeAgentModel model,
                                   HelloWorldVerifier verifier,
                                   CliDiscovery cliDiscovery) {
        return new ClaudeCodeAgentRunner(model, verifier, cliDiscovery);
    }

    @Bean
    @ConditionalOnClass(GeminiAgentModel.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    @ConditionalOnProperty(name="spring.ai.bench.agent.provider", havingValue="gemini")
    public AgentRunner geminiRunner(GeminiAgentModel model,
                                   HelloWorldVerifier verifier,
                                   CliDiscovery cliDiscovery) {
        return new GeminiAgentRunner(model, verifier, cliDiscovery);
    }
}
```

### 1.4 Spring Boot Auto-Configuration Registration
Create `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```
org.springaicommunity.bench.agents.config.AgentProviderConfig
```

### 1.5 Integration Test
Create `ClaudeCodeIntegrationTest.java`:
```java
@SpringBootTest
@Tag("agents-live")
@Tag("claude")
@ActiveProfiles("agents-live")  // Activate Spring profile for yolo settings
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@Timeout(120)  // Cap runtime at 2 minutes
class ClaudeCodeIntegrationTest {
    @Autowired
    private CliDiscovery cliDiscovery;

    @BeforeAll
    void requireCli() {
        assumeTrue(cliDiscovery.find("claude").isPresent(),
                   "Claude CLI not discovered");
    }

    @Test
    void helloWorld_case_passes() {
        // Test implementation
    }
}
```

### 1.6 Environment Setup
- **API Key**: Set `ANTHROPIC_API_KEY` environment variable
- **CLI**: Ensure CLI is installed (the **Agents discovery** will locate it via PATH, well-known install roots, or configured overrides)

## Phase 2: Gemini Agent Support

### 2.1 Dependencies
Add to `bench-agents/pom.xml`:
```xml
<dependency>
    <groupId>org.springaicommunity.agents</groupId>
    <artifactId>spring-ai-gemini</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2.2 Implementation
Create `GeminiAgentRunner.java`:
- Same structure as ClaudeCodeAgentRunner
- Uses existing `HelloWorldVerifier`
- Maps yolo mode appropriately for Gemini CLI
- **Uses zt-exec `ProcessExecutor.directory(workspace)`** to run CLI in workspace
- Resolves CLI path via Spring AI Agents CLI discovery (checks gemini and gcloud)
- Captures CLI version using zt-exec with 3s timeout
- Adds comprehensive metadata under `provenance.agent`

### 2.3 Integration Test
Create `GeminiIntegrationTest.java`:
```java
@SpringBootTest
@Tag("agents-live")
@Tag("gemini")
@ActiveProfiles("agents-live")  // Activate Spring profile
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
@Timeout(120)  // Cap runtime at 2 minutes
class GeminiIntegrationTest {
    @Autowired
    private CliDiscovery cliDiscovery;

    @BeforeAll
    void requireCli() {
        // Check for gemini or gcloud as fallback
        assumeTrue(cliDiscovery.find("gemini").isPresent() ||
                   cliDiscovery.find("gcloud").isPresent(),
                   "Gemini CLI not discovered");
    }
}
```

## Configuration

### Application Properties (Default)
Create `bench-agents/src/main/resources/application.properties`:
```properties
# Bench agent selection
spring.ai.bench.agent.provider=claude-code

# Claude settings (using SDK's standard keys)
spring.ai.agent.claude.model=claude-sonnet-4-0
spring.ai.agent.claude.timeout=PT2M
# Note: yolo NOT set here - only in agents-live profile

# Gemini settings (using SDK's standard keys)
spring.ai.agent.gemini.model=gemini-1.5-pro
spring.ai.agent.gemini.timeout=PT2M

# Verification settings
spring.ai.bench.verification.type=hello-world

# Optional: customize prompt per provider
# spring.ai.bench.hello.prompt.claude=Create a file named hello.txt...
# spring.ai.bench.hello.prompt.gemini=Create a file named hello.txt...
```

### Test Profile Properties
Create `bench-agents/src/test/resources/application-agents-live.properties`:
```properties
# YOLO/permissions only for live agent tests
spring.ai.agent.claude.yolo=true
spring.ai.agent.gemini.yolo=true
```

### Maven Profile
Add to `bench-agents/pom.xml`:
```xml
<profiles>
    <profile>
        <id>agents-live</id>
        <!-- No activation block - use explicit -P flag -->
        <build>
            <plugins>
                <plugin>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.2.5</version>
                    <configuration>
                        <properties>
                            <configurationParameters>junit.jupiter.tags=agents-live</configurationParameters>
                        </properties>
                        <systemPropertyVariables>
                            <spring.profiles.active>agents-live</spring.profiles.active>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

## Report Enhancement

Update report generation to include comprehensive provenance:
```java
Map<String, Object> provenance = new HashMap<>();
provenance.put("benchVersion", "0.1.0-SNAPSHOT");
provenance.put("agentsVersion", "0.1.0-SNAPSHOT");  // Spring AI Agents library version
provenance.put("reportFormat", "1.0");
provenance.put("generatedAt", Instant.now().toString());

// Agent info as nested object
Map<String, Object> agentInfo = new HashMap<>();
agentInfo.put("provider", "claude-code");  // or "gemini"
agentInfo.put("model", "claude-sonnet-4-0");
agentInfo.put("cliVersion", getCliVersion(cliPath, Duration.ofSeconds(3)));
agentInfo.put("cliPath", cliPath.toString());  // Resolved CLI path
agentInfo.put("timeout", "PT2M");  // Provider timeout for quick triage
agentInfo.put("workspacePath", workspace.toAbsolutePath().toString());  // Resolved path
agentInfo.put("commandLine", "claude --dangerously-skip-permissions [workspace]"); // Sanitized
provenance.put("agent", agentInfo);

report.put("provenance", provenance);
```

## CLI Utilities

Create `CliUtils.java`:
```java
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class CliUtils {
    /**
     * Get CLI version using resolved path and zt-exec
     * @param cliPath Resolved CLI path from discovery
     * @param timeout Timeout duration
     * @return Version string or "unknown"
     */
    public static String getCliVersion(Path cliPath, Duration timeout) {
        try {
            ProcessResult result = new ProcessExecutor()
                .command(cliPath.toString(), "--version")
                .readOutput(true)
                .redirectErrorStream(true)
                .timeout(timeout.toSeconds(), TimeUnit.SECONDS)
                .exitValue(0)
                .execute();

            return result.outputUTF8()
                .lines()
                .findFirst()
                .orElse("unknown")
                .trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static void logEnvironmentStatus() {
        System.out.println("Environment status:");
        System.out.println("  ANTHROPIC_API_KEY=" +
            (System.getenv("ANTHROPIC_API_KEY") != null ? "present" : "missing"));
        System.out.println("  GEMINI_API_KEY=" +
            (System.getenv("GEMINI_API_KEY") != null ? "present" : "missing"));
    }
}
```

## README

Create `bench-agents/README.md`:
```markdown
# Bench Agents Integration

Integration layer between Spring AI Bench and Spring AI Agents.

## Running Tests

```bash
# Run standard bench tests (skips live agent tests)
mvn test

# Run live agent tests with default provider (claude-code)
mvn test -Pagents-live

# Run specific provider test
mvn test -Pagents-live -Dtest=ClaudeCodeIntegrationTest
mvn test -Pagents-live -Dtest=GeminiIntegrationTest

# Switch providers dynamically
mvn test -Pagents-live -Dspring.ai.bench.agent.provider=gemini
```

## Prerequisites for Live Tests

### Claude Code
- Install Claude CLI: `npm install -g @anthropic-ai/claude-cli`
- Set environment: `export ANTHROPIC_API_KEY=your-key`
- CLI will be auto-discovered via PATH or well-known locations

### Gemini
- Install Gemini CLI or gcloud
- Set environment: `export GEMINI_API_KEY=your-key`
- CLI will be auto-discovered (checks gemini and gcloud)

## Configuration

Agent selection via `spring.ai.bench.agent.provider`:
- `claude-code` - Claude Code CLI agent
- `gemini` - Gemini CLI agent
- `hello-world` - Mock agent for testing

## Architecture

```
┌─────────────────────────┐
│   Spring AI Bench       │
│   (Test Framework)      │
└───────────┬─────────────┘
            │
┌───────────▼─────────────┐
│   bench-agents          │
│   (Integration Layer)   │
│   - AgentRunners        │
│   - Auto-Configuration  │
│   - Uses zt-exec        │
└───────────┬─────────────┘
            │
┌───────────▼─────────────┐
│   Spring AI Agents      │
│   - ClaudeCodeAgentModel│
│   - GeminiAgentModel    │
│   - CLI Discovery       │
└─────────────────────────┘
```

## Technical Notes

- All CLI processes launched via **zt-exec** for consistent handling
- CLI discovery via **spring-ai-agent utilities** (no ad-hoc PATH checks)
- Process working directory set via `ProcessExecutor.directory()`
- Comprehensive timeout and error handling
```

## Testing Commands

```bash
# Default: runs bench tests, SKIPS live agent tests
mvn test

# Run with live agents using explicit profile
mvn test -Pagents-live

# Run specific provider tests
mvn test -Pagents-live -Dtest=ClaudeCodeIntegrationTest
mvn test -Pagents-live -Dtest=GeminiIntegrationTest

# Switch providers
mvn test -Pagents-live -Dspring.ai.bench.agent.provider=gemini
```

## Files to Create

1. **Runners**:
   - `bench-agents/src/main/java/org/springaicommunity/bench/agents/claudecode/ClaudeCodeAgentRunner.java`
   - `bench-agents/src/main/java/org/springaicommunity/bench/agents/gemini/GeminiAgentRunner.java`

2. **Configuration**:
   - `bench-agents/src/main/java/org/springaicommunity/bench/agents/config/AgentProviderConfig.java`
   - `bench-agents/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - `bench-agents/src/main/resources/application.properties`
   - `bench-agents/src/test/resources/application-agents-live.properties`

3. **Utilities**:
   - `bench-agents/src/main/java/org/springaicommunity/bench/agents/util/CliUtils.java`

4. **Tests**:
   - `bench-agents/src/test/java/org/springaicommunity/bench/agents/claudecode/ClaudeCodeIntegrationTest.java`
   - `bench-agents/src/test/java/org/springaicommunity/bench/agents/gemini/GeminiIntegrationTest.java`

5. **Documentation**:
   - `bench-agents/README.md`

## Acceptance Criteria

### Phase 1 (Claude):
- ✅ `mvn test` runs bench tests, skips live agent tests
- ✅ `mvn test -Pagents-live` runs live tests with Spring context and profile
- ✅ Live tests skip when env/CLI missing (shows "skipped" not "failed")
- ✅ Test runtime capped at 120 seconds to prevent hangs
- ✅ hello-world case PASS with ClaudeCodeAgentRunner using `yolo=true` (via Spring profile)
- ✅ Report includes comprehensive `provenance.agent` metadata with CLI path
- ✅ **All CLI processes launched via zt-exec** (no direct ProcessBuilder)
- ✅ **CLI location resolved via spring-ai-agent discovery utilities** (no ad-hoc PATH probing)
- ✅ No secrets leaked in logs (only presence/absence)
- ✅ Auto-configuration discovered via Spring Boot 3 mechanism

### Phase 2 (Gemini):
- ✅ Same hello case PASS with GeminiAgentRunner
- ✅ Live test gated via CLI discovery (gemini or gcloud)
- ✅ Report includes comprehensive `provenance.agent` metadata
- ✅ Uses identical HelloWorldVerifier
- ✅ Test runtime capped at 120 seconds
- ✅ **All processes use zt-exec with consistent error handling**
- ✅ **CLI discovery handles fallback to gcloud**

## Implementation Notes

1. **Process Execution**:
   - **All CLI execution via zt-exec ProcessExecutor**
   - Set working directory: `ProcessExecutor.directory(workspace.toFile())`
   - Consistent timeout and error handling
   - Stream output for debugging

2. **CLI Discovery**:
   - Use Spring AI Agents `CliDiscovery` service
   - Resolves CLI via PATH, well-known roots, or config overrides
   - Returns `Optional<Path>` for graceful test skipping
   - `require()` throws with helpful message for runtime

3. **Spring Boot Auto-Configuration**:
   - Register via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   - Ensures config is discovered even outside component scan
   - Boot 3 standard mechanism

4. **Spring Test Context**:
   - Tests use `@SpringBootTest` with `@ActiveProfiles("agents-live")`
   - Autowire `CliDiscovery` for CLI resolution
   - Ensures Spring profile properties are loaded

5. **CLI Version Capture**:
   - Uses zt-exec with resolved CLI path
   - UTF-8 output handling
   - Returns "unknown" without blocking
   - First line only for consistency

6. **Permission Handling**:
   - Claude: Map `yolo=true` to `--dangerously-skip-permissions`
   - Gemini: Enable equivalent permissionless mode
   - YOLO only via Spring profile, never in default config

7. **Report Metadata Enhanced**:
   - Include resolved CLI path from discovery
   - Include provider timeout for quick triage
   - Include resolved workspace path
   - Include both benchVersion and agentsVersion
   - Sanitized command line for reproducibility

## Dependencies on Spring AI Agents

This implementation depends on the Spring AI Agents project structure:
- Core abstractions: `AgentModel`, `AgentTaskRequest`, `AgentResponse`
- Provider implementations: `ClaudeCodeAgentModel`, `GeminiAgentModel`
- SDK clients: `ClaudeCodeClient`, `GeminiClient`
- **CLI discovery utilities: `CliDiscovery` service**
- **zt-exec for process execution**

References:
- [Claude Code SDK Documentation](https://spring-ai-community.github.io/spring-ai-agents/api/claude-code-sdk.html)
- [Gemini CLI SDK Documentation](https://spring-ai-community.github.io/spring-ai-agents/api/gemini-cli-sdk.html)
- [Claude CLI Reference](https://docs.anthropic.com/en/docs/claude-code/cli-reference)
- [Gemini CLI Authentication](https://google-gemini.github.io/gemini-cli/docs/cli/authentication.html)
- [zt-exec Documentation](https://github.com/zeroturnaround/zt-exec)

## Status

✅ **APPROVED** - Ready for implementation with all requirements addressed:
- zt-exec for all process execution (no ProcessBuilder)
- Spring AI Agents CLI discovery utilities
- Spring Boot auto-configuration registration
- Spring test context with @SpringBootTest
- Comprehensive provenance in reports
- Clear documentation with provider switching examples