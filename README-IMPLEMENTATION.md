# Spring AI Bench

A comprehensive **execution framework and benchmarking platform** for AI agents in Java environments. Spring AI Bench provides isolated sandboxes, customizable execution, and evaluation capabilities for testing AI agent performance on real-world software engineering tasks.

[![Build Status](https://github.com/spring-ai-community/spring-ai-bench/workflows/CI/badge.svg)](https://github.com/spring-ai-community/spring-ai-bench/actions)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-orange.svg)](https://maven.apache.org/)

## Quick Start

```bash
# Clone and build
git clone https://github.com/spring-ai-community/spring-ai-bench.git
cd spring-ai-bench
./mvnw clean install

# Run a sample benchmark
./mvnw test -Dtest=BenchHarnessE2ETest
```

## Overview

Spring AI Bench is designed to evaluate AI agents across different execution environments:
- **Local Process Execution** - Fast local testing and development
- **Docker Sandbox** - Isolated container execution with TestContainers
- **Cloud Runtime** - Scalable distributed execution (future)

### Key Features

- ✅ **Multiple Execution Backends** - Local, Docker, and cloud-ready sandboxes
- ✅ **Spring AI Agents Integration** - Native integration with Spring AI Agents framework
- ✅ **Claude 4 Sonnet Support** - Advanced AI agent execution with cost and token tracking
- ✅ **Enhanced HTML Reporting** - Rich reports with execution metadata, costs, and performance metrics
- ✅ **Agent Integration** - Claude Code, Goose, OpenAI Codex support
- ✅ **Robust Process Management** - Powered by zt-exec library
- ✅ **Customizable Benchmarks** - YAML-based benchmark specifications
- ✅ **GitHub Integration** - Automatic repository cloning and workspace management
- ✅ **MCP Protocol Support** - Model Context Protocol tool integration
- ✅ **Comprehensive Testing** - Unit, integration, and E2E test coverage

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   BenchHarness  │    │   AgentRunner   │    │SuccessVerifier │
│                 │    │                 │    │                 │
│ Orchestrates    │────│ Executes agent  │────│ Validates       │
│ benchmark runs  │    │ in workspace    │    │ success criteria│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │    Sandbox      │
                       │   (Interface)   │
                       └─────────────────┘
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
        ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
        │LocalSandbox │ │DockerSandbox│ │CloudSandbox │
        │             │ │             │ │   (Future)  │
        │Process exec │ │Containers   │ │Distributed  │
        └─────────────┘ └─────────────┘ └─────────────┘
```

## Usage Examples

### Basic Local Execution

```java
// Execute a simple command
try (var sandbox = LocalSandbox.builder().build()) {
    var spec = ExecSpec.of("echo", "Hello World");
    var result = sandbox.exec(spec);

    System.out.println("Exit code: " + result.exitCode());
    System.out.println("Output: " + result.mergedLog());
}
```

### Docker Sandbox with Custom Environment

```java
// Execute in isolated Docker container
try (var sandbox = new DockerSandbox("openjdk:17-jdk")) {
    var spec = ExecSpec.builder()
        .command("java", "-version")
        .env("JAVA_OPTS", "-Xmx512m")
        .timeout(Duration.ofSeconds(30))
        .build();

    var result = sandbox.exec(spec);
    System.out.println("Java version: " + result.mergedLog());
}
```

### Running a Complete Benchmark

```java
// Load and execute a benchmark
Path benchmarkFile = Path.of("benchmarks/calculator-sqrt-bug.yaml");
BenchCase benchCase = BenchCaseLoader.load(benchmarkFile);

var harness = new BenchHarness(github, Duration.ofMinutes(10));
BenchResult result = harness.run(benchCase);

System.out.println("Success: " + result.success());
System.out.println("Duration: " + result.duration());
```

## Benchmark Specification Format

Benchmarks are defined in YAML files with the following structure:

```yaml
# calculator-sqrt-bug.yaml
id: calculator-sqrt-bug
category: coding
version: 0.5

repo:
  owner: rd-1-2022
  name: simple-calculator
  ref: 93da3b1847ed67f3bc7d8a84e1e6afd737f1a555

agent:
  kind: claude-code
  model: claude-4-sonnet
  autoApprove: true
  prompt: |
    Fix the failing JUnit tests in this project.
    Run "./mvnw test" until all tests pass, then commit.

success:
  cmd: mvn test

timeoutSec: 600
```

### Supported Agent Types

- **`claude-code`** - Claude Code CLI integration with MCP tools ✅
- **`gemini`** - Google Gemini CLI integration with yolo mode ✅
- **`hello-world`** - Mock agent for testing infrastructure ✅

### Benchmark Categories

- **`coding`** - Software development tasks (bug fixes, feature implementation)
- **`project-mgmt`** - Project management and planning tasks
- **`version-upgrade`** - Dependency and framework upgrade tasks

## Development Setup

### Prerequisites

- **Java 17+** - Required for building and running
- **Maven 3.6+** - Build system
- **Docker** - For DockerSandbox testing (optional)
- **GitHub Token** - For repository access (set `GITHUB_TOKEN` env var)
- **Agent API Keys** - For agent integration tests:
  - `ANTHROPIC_API_KEY` - Claude Code agent
  - `GEMINI_API_KEY` - Gemini agent

### Building from Source

```bash
# Full build with tests
./mvnw clean install

# Quick build (skip tests)
./mvnw clean install -DskipTests

# Run specific test categories
./mvnw test -Dtest=*IntegrationTest
./mvnw test -Dtest=BenchHarnessE2ETest
```

### Running Tests

```bash
# All tests
./mvnw test

# Integration tests only
./mvnw test -Dtest=*IntegrationTest

# Specific sandbox tests
./mvnw test -Dtest=LocalSandboxIntegrationTest
./mvnw test -Dtest=DockerSandboxTest

# Agent integration tests (requires API keys)
ANTHROPIC_API_KEY=your_key GEMINI_API_KEY=your_key ./mvnw test -Pagents-live
# Or run specific agent tests:
ANTHROPIC_API_KEY=your_key ./mvnw test -Dtest=ClaudeIntegrationTest
GEMINI_API_KEY=your_key ./mvnw test -Dtest=GeminiIntegrationTest
```

## Configuration

### Environment Variables

```bash
# GitHub API access
export GITHUB_TOKEN=your_github_token

# MCP tools configuration (automatically set by framework)
export MCP_TOOLS=brave,filesystem,github

# Docker settings (for DockerSandbox)
export DOCKER_HOST=unix:///var/run/docker.sock
```

### JVM Configuration

The project includes optimized JVM settings in `.mvn/jvm.config`:

```
-XX:-PrintWarnings
-Xshare:off
```

## Advanced Features

### Custom Execution Customizers

```java
// Create a customizer for Claude CLI integration
ExecSpecCustomizer claudeCustomizer = new ClaudeCliCustomizer();

// Build sandbox with customizers
var sandbox = LocalSandbox.builder()
    .customizer(claudeCustomizer)
    .workingDirectory(workspace)
    .build();
```

### MCP (Model Context Protocol) Integration

```java
// Configure MCP tools
var mcpConfig = McpConfig.of("brave", "filesystem", "github");

var spec = ExecSpec.builder()
    .command("claude-code", "agent.py")
    .mcp(mcpConfig)  // Automatically adds --tools=brave,filesystem,github
    .build();
```

### Workspace Management

```java
// Automatic repository cloning and cleanup
try (var manager = new RepoWorkspaceManager(github)) {
    var repoSpec = new RepoSpec("owner", "repo", "main");
    var workspace = manager.checkout(repoSpec, Duration.ofMinutes(5));

    // Use workspace for agent execution
    var sandbox = LocalSandbox.builder()
        .workingDirectory(workspace.repoDir())
        .build();
}
```

## Testing Strategy

Spring AI Bench uses a comprehensive testing approach:

- **Unit Tests** - Individual component testing (90 tests)
- **Integration Tests** - Real process execution validation (17 tests)
- **E2E Tests** - Complete benchmark workflow testing (2 tests)
- **Smoke Tests** - Basic functionality validation (1 test)

Total: **174 tests** with 100% pass rate

## Recent Updates

### v0.1.0-SNAPSHOT (Latest)
- ✅ **Migrated to zt-exec** - Replaced ProcessBuilder with robust zt-exec library
- ✅ **Improved timeout handling** - Automatic process destruction and better error messages
- ✅ **Enhanced platform compatibility** - Windows/Unix shell command abstraction
- ✅ **Simplified test suite** - Removed complex mocks, focus on integration testing
- ✅ **Updated naming** - Changed from "swe-bench" to "sai-bench" prefixes

### Key Improvements
- **Cleaner Process Execution** - Fluent zt-exec API vs manual ProcessBuilder management
- **Better Error Handling** - Detailed error messages with command output
- **Robust Timeout Management** - Built-in process cleanup and timeout exceptions
- **Future-Ready Logging** - Native SLF4J integration for observability

## Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Detailed technical architecture and design decisions
- **[future-plans.md](./future-plans.md)** - Cloud migration roadmap and enterprise features
- **[LICENSE](./LICENSE)** - Apache License 2.0

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Spring Java formatting conventions
- Write tests for new features
- Update documentation for API changes
- Ensure all tests pass: `./mvnw clean test`

## Support

- **Issues**: [GitHub Issues](https://github.com/spring-ai-community/spring-ai-bench/issues)
- **Discussions**: [GitHub Discussions](https://github.com/spring-ai-community/spring-ai-bench/discussions)
- **Documentation**: [Wiki](https://github.com/spring-ai-community/spring-ai-bench/wiki)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built on the **Spring Framework** ecosystem
- Inspired by **SWE-bench** for AI agent evaluation
- Process execution powered by **zt-exec** library
- Container isolation via **TestContainers**
- Follows **Testcontainers Cloud** business model principles

---

**Spring AI Bench** - *Benchmarking AI agents for real-world software engineering tasks*