# Spring AI Bench - Architecture & Design Documentation

## Overview

Spring AI Bench is a comprehensive execution framework for running AI agents in isolated environments with support for benchmarking, customization, and monitoring. The project implements a runtime system that supports multiple execution backends: **process exec**, **testcontainers**, and provides the foundation for **distributed AWS-based architecture**.

## Connection to SWE Agent

This project is based on and connected to the **SWE (Software Engineering) agent distributed runtime** system, as evidenced by:
- Temporary directory naming: `"swe-bench-"` prefix in `LocalSandbox.java:232`
- Architecture designed for AI agent evaluation and benchmarking
- Integration with software engineering task automation

## Core Architecture

### 1. Execution Framework

The system is built around a **Sandbox abstraction** that provides isolated execution environments:

```
Sandbox (interface)
├── LocalSandbox - Process exec implementation
├── DockerSandbox - Testcontainers implementation  
└── [Future: AWS/Cloud implementations]
```

**Key Components:**
- `ExecSpec` - Command specification with timeout, environment variables, MCP config
- `ExecResult` - Execution results with exit codes, logs, duration
- `TimeoutException` - Timeout handling for long-running processes

### 2. Execution Backends

#### Local Process Execution (`LocalSandbox`)
- **Purpose**: Execute commands in local processes within isolated directories
- **Security**: Directory isolation only - commands execute with JVM privileges
- **Features**: 
  - Customizable working directories
  - Environment variable support
  - Timeout handling
  - MCP (Model Context Protocol) integration
  - Automatic cleanup of temporary directories

#### Docker/Testcontainers (`DockerSandbox`) 
- **Purpose**: Execute commands in Docker containers for strong isolation
- **Features**:
  - Uses TestContainers library (v1.21.0)
  - Long-lived containers with "sleep infinity" pattern
  - Multiple command executions within same container environment
  - Automatic container lifecycle management
  - Working directory: `/work`

#### Future: Distributed/AWS Implementation
- Foundation laid with **Spring Cloud Deployer SPI** integration
- Support for distributed task execution
- Prepared for AWS-based distributed architecture

### 3. Customization Framework

**ExecSpecCustomizer Pattern** allows runtime modification of execution specifications:

- `ExecSpecCustomizer` (interface) - Base customization contract
- `ClaudeCliCustomizer` - Specialized for Claude CLI integration
  - Automatically injects MCP tools via `--tools` flag
  - Transforms: `["claude-cli", "agent.py"]` → `["claude-cli", "agent.py", "--tools=brave,filesystem"]`

### 4. Repository & Workspace Management

- `RepoWorkspaceManager` - GitHub repository operations
- `Workspace` - Isolated workspace for agent execution
- Automatic repository cloning and cleanup
- GitHub API integration for repository access

### 5. Benchmarking System

#### Benchmark Specifications
- `BenchSpec` - Top-level benchmark specification
- `BenchCase` - Individual benchmark case with:
  - ID, category ("coding", "project-mgmt", "version-upgrade")
  - Repository specification (`RepoSpec`)
  - Agent specification (`AgentSpec`)
  - Success criteria (`SuccessSpec`)
  - Timeout configuration

#### Agent Support
`AgentSpec` supports multiple agent types:
- `"claude-code"` - Claude Code integration
- `"goose"` - Goose agent
- `"openai-codex"` - OpenAI Codex
- Configurable models, prompts, generation parameters

#### Execution Harness
- `BenchHarness` - End-to-end benchmark execution
- `AgentRunner` - Agent execution interface
- `DummyPatchRunner` - Test implementation
- `SuccessVerifier` - Validation of benchmark results

### 6. Integration Components

#### Spring Cloud Deployer
- **SPI Integration**: `spring-cloud-deployer-spi` (v2.9.5)
- **Local Implementation**: `spring-cloud-deployer-local` (v2.9.5)  
- **Purpose**: Process management and distributed task execution
- **Usage**: `LocalTaskLauncher` for process orchestration

#### Model Context Protocol (MCP)
- `McpConfig` - Configuration for MCP tool integration
- Environment variable injection: `MCP_TOOLS`
- Integration with Claude CLI through customizers

#### TestContainers
- **Version**: 1.21.0
- **Purpose**: Docker-based sandbox isolation
- **Features**: Container lifecycle management, port mapping, volume mounts

## Key Design Decisions

### 1. Sandbox Abstraction
- **Rationale**: Support multiple execution environments (local, Docker, future cloud)
- **Pattern**: Interface-based design for extensibility
- **Trade-offs**: Abstraction overhead vs. flexibility

### 2. Merged Log Output
- **Design**: `ExecResult` combines stdout/stderr into `mergedLog`
- **Rationale**: Optimized for AI analysis - preserves temporal ordering
- **Use Case**: LLMs can analyze execution logs in chronological order

### 3. Customizer Pattern
- **Purpose**: Last-mile command/environment customization
- **Benefits**: Flexible, composable, testable
- **Example**: Claude CLI tool injection without hardcoding

### 4. Resource Management
- **AutoCloseable**: All sandboxes implement proper cleanup
- **Try-with-resources**: Workspace management ensures cleanup
- **Timeout Handling**: Prevents runaway processes

## Project Timeline

**July 2025 Development:**
- **July 1, 2025**: Major implementation commit (`e253581`)
  - "feat: add comprehensive execution framework with sandbox isolation"
  - Complete execution framework implementation
  - Sandbox implementations (Local + Docker)
  - Customizer pattern and MCP integration
  - Benchmark harness and evaluation system

## Dependencies & Technology Stack

### Core Dependencies
- **Spring Framework**: Core dependency injection and configuration
- **Spring Cloud Deployer**: Distributed process management
- **Jackson**: YAML/JSON configuration handling
- **TestContainers**: Docker sandbox implementation
- **GitHub API**: Repository operations
- **SLF4J**: Logging framework

### Build System
- **Maven**: Build system with multi-module structure
- **Java 17+**: Target runtime
- **Surefire**: Test execution

## Module Structure

```
spring-ai-bench/
├── bench-core/           # Core execution framework
│   ├── exec/            # Execution system (Sandbox, ExecSpec, etc.)
│   ├── spec/            # Benchmark specifications
│   ├── repo/            # Repository & workspace management
│   ├── run/             # Benchmark harness & execution
│   └── io/              # Configuration loading
└── bench-app/           # Application layer (future)
```

## Future Development Areas

### 1. AWS Distributed Implementation
- Cloud-based sandbox implementations
- Auto-scaling execution clusters
- Distributed benchmark orchestration
- Cost optimization strategies

### 2. Enhanced Agent Support
- Additional agent integrations beyond Claude CLI
- Agent-specific optimizations and customizations
- Multi-agent benchmark scenarios

### 3. Monitoring & Observability
- Execution metrics collection
- Performance monitoring dashboards
- Resource utilization tracking
- Benchmark result analytics

### 4. Security Enhancements
- Improved sandbox isolation
- Resource limits and quotas
- Security scanning integration
- Audit logging

## Getting Started

### Prerequisites
- Java 17+
- Docker (for DockerSandbox)
- Maven 3.6+
- GitHub access token (for repository operations)

### Basic Usage

```java
// Local execution
try (var sandbox = LocalSandbox.builder().build()) {
    var spec = ExecSpec.of("echo", "Hello World");
    var result = sandbox.exec(spec);
    System.out.println("Exit code: " + result.exitCode());
    System.out.println("Output: " + result.mergedLog());
}

// Docker execution  
try (var sandbox = new DockerSandbox("openjdk:17-jdk")) {
    var spec = ExecSpec.of("java", "-version");
    var result = sandbox.exec(spec);
    System.out.println("Java version: " + result.mergedLog());
}
```

## Testing Strategy

- **Unit Tests**: Individual component testing
- **Integration Tests**: End-to-end sandbox execution
- **Smoke Tests**: Basic functionality validation
- **E2E Tests**: Complete benchmark execution flows

---

*This document was generated on 2025-09-05 based on analysis of the codebase and git history.*