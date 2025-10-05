# Bench Agents Integration

Integration layer between Spring AI Bench and Spring AI Agents, enabling benchmarking of real AI agent implementations.

## Overview

This module provides a bridge between the Spring AI Bench testing framework and the Spring AI Agents project, allowing you to run benchmarks against actual AI agents like Claude Code and Gemini.

## Quick Start

### Prerequisites

For live agent testing, you'll need:

**Claude Code:**
- Install Claude CLI: `npm install -g @anthropic-ai/claude-cli`
- Set environment: `export ANTHROPIC_API_KEY=your-key`

**Gemini:**
- Install Gemini CLI or gcloud SDK
- Set environment: `export GEMINI_API_KEY=your-key`

### Running Tests

```bash
# Run standard bench tests (skips live agent tests)
mvn test

# Run live agent tests with default provider (claude-code)
mvn test -Pagents-live

# Run specific provider test
mvn test -Pagents-live -Dtest=ClaudeCodeIntegrationTest

# Switch providers dynamically
mvn test -Pagents-live -Dspring.ai.bench.agent.provider=gemini
```

## Configuration

### Agent Selection

Control which agent to use via the `spring.ai.bench.agent.provider` property:

- `claude-code` - Claude Code CLI agent
- `gemini` - Gemini CLI agent
- `hello-world` - Mock agent for testing (default fallback)

### Provider Settings

The module uses standard Spring AI Agents configuration keys:

```properties
# Claude settings
spring.ai.agent.claude.model=claude-sonnet-4-0
spring.ai.agent.claude.timeout=PT2M

# Gemini settings
spring.ai.agent.gemini.model=gemini-2.0-flash-exp
spring.ai.agent.gemini.timeout=PT2M
```

### Test Profile

Live agent tests use the `agents-live` Spring profile which enables:
- `spring.ai.agent.claude.yolo=true` - Skip permission prompts
- `spring.ai.agent.gemini.yolo=true` - Skip permission prompts

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

### Key Components

- **AgentRunners**: Wrapper implementations that bridge Spring AI Bench and Spring AI Agents APIs
- **AgentModelAdapter**: Enhanced adapter with service extraction and workspace-specific configuration
- **WorkspaceService**: Handles workspace cleanup and management operations
- **ReportService**: Generates HTML and JSON reports with comprehensive metadata
- **Auto-Configuration**: Spring Boot auto-configuration for conditional bean creation
- **CLI Utils**: Utilities for CLI version detection using zt-exec

## Technical Details

### AgentSpec Builder Pattern

The module now supports a fluent builder API for creating agent specifications:

```java
AgentSpec spec = AgentSpec.builder()
    .kind("hello-world")
    .model("claude-3-5-sonnet")
    .prompt("Create a file named hello.txt with contents: Hello World!")
    .autoApprove(true)
    .build();
```

### Service Architecture

The module follows Spring best practices with service extraction:

- **WorkspaceService**: Handles workspace lifecycle management and cleanup
- **ReportService**: Centralized report generation with HTML and JSON outputs
- **AgentModelAdapter**: Simplified adapter focused on orchestration
- **Factory Methods**: Workspace-specific agent model configuration

### Process Execution

- All CLI processes launched via **zt-exec ProcessExecutor** for consistent handling
- Working directory set via `ProcessExecutor.directory(workspace)`
- Comprehensive timeout and error handling

### CLI Discovery

- Uses Spring AI Agents `CliDiscovery` service
- Resolves CLI via PATH, well-known roots, or config overrides
- Graceful fallback for missing CLIs (tests are skipped, not failed)

### Report Enhancement

Reports include comprehensive provenance metadata:

```json
{
  "provenance": {
    "benchVersion": "0.1.0-SNAPSHOT",
    "agentsVersion": "0.1.0-SNAPSHOT",
    "agent": {
      "provider": "claude-code",
      "workspacePath": "/path/to/workspace"
    }
  }
}
```

### Security

- No secrets leaked in logs (only presence/absence reported)
- Sandbox isolation via workspace directories
- Permission-bypassing only in test profiles

## Development

### Project Structure

```
bench-agents/
├── src/main/java/
│   ├── runner/          # Agent runners, services, and configuration
│   │   ├── AgentModelAdapter.java    # Enhanced adapter with service extraction
│   │   ├── ClaudeCodeAgentRunner.java
│   │   ├── GeminiAgentRunner.java
│   │   ├── AgentProviderConfig.java  # Spring auto-configuration
│   │   ├── WorkspaceService.java     # Workspace management
│   │   └── ReportService.java        # Report generation
│   ├── support/         # Utilities and shared support classes
│   │   ├── CliUtils.java
│   │   └── SimpleLogCapture.java
│   ├── report/          # HTML and JSON report generators
│   └── judge/           # Judge framework integration for benchmarks
├── src/main/resources/
│   ├── META-INF/spring/ # Auto-configuration registration
│   └── application.properties
└── src/test/
    ├── java/            # Integration tests
    └── resources/       # Test profiles
```

### Adding New Providers

1. Add dependency to `pom.xml`
2. Create `{Provider}AgentRunner` implementing `AgentRunner`
3. Add conditional bean to `AgentProviderConfig`
4. Create integration test with proper gating
5. Update configuration documentation

### Testing Strategy

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test full agent execution with gating
  - `@Tag("agents-live")` for live agent tests
  - `@EnabledIfEnvironmentVariable` for API key checks
  - `@Timeout(120)` to prevent hangs
  - `@SpringBootTest` with `@ActiveProfiles("agents-live")`

## Dependencies

This module depends on:

- **Spring AI Bench Core**: Test framework and execution APIs
- **Spring AI Agents**: Agent models and CLI utilities
- **zt-exec**: Process execution library
- **Spring Boot**: Auto-configuration and testing support

## License

Licensed under the Apache License, Version 2.0.