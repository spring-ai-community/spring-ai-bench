# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring AI Bench is a comprehensive benchmarking suite for Java-centric AI developer agents. It provides isolated sandboxes, customizable execution, and evaluation capabilities for testing AI agent performance on real-world software engineering tasks.

## Architecture

The project follows a multi-module Maven structure with clear separation of concerns:

- **bench-core**: Core CLI runner and execution engine (`BenchMain`, `BenchRunner`)
- **bench-agents**: Agent integration layer with Spring Boot auto-configuration
- **bench-site**: Static site generator for benchmark results
- **bench-app**: Main application entry point
- **docs**: AsciiDoc documentation with Antora

### Key Integration Pattern

The system integrates with external agents via **JBang invocation** following this pattern:
```bash
jbang /path/to/spring-ai-agents/jbang/launcher.java <agent-name> key=value key2=value2
```

This ensures benchmark success guarantees good end-user experience by testing the exact CLI interface users would use.

### Configuration System

The system uses a two-tier YAML configuration:

1. **Case specs** (`bench-tracks/<case>/cases/<case>.yaml`) - Static benchmark definitions
2. **Run specs** (`runs/examples/<case>-run.yaml`) - Per-invocation configuration

Configuration merging precedence: Case → Run YAML → CLI overrides

## Essential Commands

### Building and Testing
```bash
# Full build and test
./mvnw clean install

# Build without tests (fastest for development)
./mvnw clean install -DskipTests

# Apply Spring Java Format (required before commits)
./mvnw spring-javaformat:apply
```

### Git Commit Guidelines
- **NEVER add Claude Code attribution** in commit messages
- Keep commit messages clean and professional without AI attribution

### Running Benchmarks
```bash
# Run hello-world benchmark via Maven exec plugin
./mvnw exec:java -pl bench-core -Dexec.args="run --run-file runs/examples/hello-world-run.yaml"

# Generate static site from benchmark results (JBang - much simpler!)
jbang jbang/site.java --reportsDir /tmp/bench-reports --siteDir /tmp/bench-site

# Alternative: Maven exec (more verbose)
./mvnw exec:java -pl bench-site -Dexec.mainClass="org.springaicommunity.bench.site.SiteMain" \
  -Dexec.args="--reportsDir /tmp/bench-reports --siteDir /tmp/bench-site"
```

### Agent Integration Testing
```bash
# Core tests (no API keys required)
./mvnw test

# Live agent tests (requires API keys)
export ANTHROPIC_API_KEY=your_key
export GEMINI_API_KEY=your_key
./mvnw test -Pagents-live

# Multi-agent comparison test (runs deterministic + AI agents)
ANTHROPIC_API_KEY=your_key GEMINI_API_KEY=your_key \
./mvnw test -Dtest=HelloWorldMultiAgentTest -pl bench-agents

# Specific agent tests
./mvnw test -Dtest=ClaudeCodeIntegrationTest
./mvnw test -Dtest=GeminiIntegrationTest
./mvnw test -Dtest=HelloWorldIntegrationTest
./mvnw test -Dtest=HelloWorldAIIntegrationTest -pl bench-agents
```

### Viewing Benchmark Reports

After running tests or benchmarks, reports are generated in multiple formats:

#### HTML Reports (for browser viewing)
```bash
# Main benchmark reports index
file:///tmp/bench-reports/index.html

# Generated benchmark site (after running site generator)
file:///tmp/bench-site/index.html

# Individual run reports (replace <run-id> with actual UUID)
file:///tmp/bench-reports/<run-id>/index.html

# Latest run in generated site
file:///tmp/bench-site/runs/<run-id>/index.html
```

#### Site Generation Process
```bash
# 1. Run tests/benchmarks to generate reports in /tmp/bench-reports
./mvnw test -Dtest=HelloWorldMultiAgentTest -pl bench-agents

# 2. Generate comprehensive site from all reports (JBang - simple!)
jbang jbang/site.java --reportsDir /tmp/bench-reports --siteDir /tmp/bench-site

# 3. Open site in browser
open file:///tmp/bench-site/index.html
```

#### Report Structure
- **Individual Reports**: Each test run creates `/tmp/bench-reports/<uuid>/`
  - `index.html` - Human-readable report with execution details
  - `report.json` - Machine-readable benchmark data
  - `run.log` - Detailed execution logs
- **Site Reports**: Aggregated view of all runs with navigation

## Maven Module Dependencies

The dependency flow is: `bench-app` → `bench-core` → `bench-agents` → external spring-ai-agents

- **bench-core** contains the CLI and execution engine
- **bench-agents** provides Spring Boot auto-configuration for agent types
- **bench-site** is independent and generates static sites from reports

## Agent Integration Architecture

The `bench-agents` module uses Spring Boot auto-configuration to wire agent types:

- `AgentProviderConfig` auto-configures runners based on `spring.ai.bench.agent.provider`
- Each agent type (claude-code, gemini, hello-world) has its own runner implementation
- Runners execute agents via **zt-exec ProcessExecutor** for safe command execution
- All CLI discovery is handled by spring-ai-agents utilities

### Agent Configuration Properties
```properties
spring.ai.bench.agent.provider=claude-code  # or gemini, hello-world
spring.ai.agent.claude.model=claude-sonnet-4-0
spring.ai.agent.claude.timeout=PT2M
spring.ai.agent.claude.yolo=true  # Only in test profiles
```

## Reporting System

The system generates three output formats per run:
- `report.json` - Machine-readable benchmark data (schema v0.1)
- `report.html` - Human-readable HTML report with navigation
- `logs.txt` - Detailed execution logs with UTC timestamps and phase tags

Reports are stored in `bench-reports/<runId>/` and the static site generator (`bench-site`) scans this directory to create an interactive web interface.

## Implementation Learnings

### Critical JBang Integration Details

**Absolute Paths Required**: JBang invocation must use absolute paths, not relative paths. The working directory during execution differs from the repository root:
```java
// ✅ Correct - absolute path
String launcherPath = "/home/user/spring-ai-agents/jbang/launcher.java";

// ❌ Incorrect - relative paths fail when executed from workspace
String launcherPath = "../spring-ai-agents/jbang/launcher.java";
```

**Command Syntax**: JBang agent invocation follows `key=value` format, NOT `--key value`:
```bash
# ✅ Correct
jbang launcher.java hello-world path=workspace/hello.txt content="Hello World!"

# ❌ Incorrect
jbang launcher.java hello-world --path workspace/hello.txt --content "Hello World!"
```

### Static Site Generator CORS Solution

**Local File Viewing Issue**: Using `fetch('runs.json')` fails when opening HTML files directly in browsers due to CORS restrictions.

**Solution**: Embed runs data directly in HTML:
```javascript
// ✅ Embedded data works locally and on web
const runs = [{"runId":"test-1","status":"success"}];

// ❌ Fetch fails for file:// protocol
fetch('runs.json').then(...)
```

### Spring Java Format Integration

**Mandatory Before Commits**: The build enforces Spring Java Format validation. Development workflow:
1. Make code changes
2. Run `./mvnw spring-javaformat:apply`
3. Commit (or CI will fail)

**Text Block Limitations**: Java text blocks cannot contain string concatenation. Use regular string concatenation for dynamic content:
```java
// ❌ Syntax error
String html = """
    <script>const data = """ + jsonData + """;</script>
    """;

// ✅ Correct approach
String html = "<script>const data = " + jsonData + ";</script>";
```

### Configuration System Patterns

**YAML Merging Precedence**: Critical for understanding configuration resolution:
1. Case YAML defines base configuration
2. Run YAML overlays/overrides case settings
3. CLI arguments override both

**Maven Exec Plugin**: The CLI is invoked via Maven exec plugin, not directly:
```bash
# ✅ Correct way to run benchmarks
./mvnw exec:java -pl bench-core -Dexec.args="run --run-file runs/examples/hello-world-run.yaml"

# ❌ Direct execution won't work without classpath setup
java -cp ... org.springaicommunity.bench.core.cli.BenchMain run ...
```

### Workspace Management

**Clean Isolation**: Each benchmark run gets a clean workspace in `bench-reports/<runId>/workspace/`. The system:
- Deletes existing workspace before agent execution
- Creates fresh directory structure
- Preserves artifacts after execution for analysis

### Process Execution Patterns

**ProcessBuilder with Absolute Paths**: Agent execution requires careful path management:
```java
ProcessBuilder pb = new ProcessBuilder(command);
pb.directory(workspaceDir.toFile()); // Set working directory
pb.redirectErrorStream(true); // Capture stderr with stdout
```

**Timeout Handling**: All agent executions have configurable timeouts to prevent hanging builds.

## Development Patterns

### Spring Java Format
This project enforces Spring Java Format. Always run `./mvnw spring-javaformat:apply` before committing. The CI pipeline will fail if formatting violations exist.

**Common Issues**:
- Long string concatenations get reformatted across multiple lines
- Text blocks have formatting restrictions
- Import organization follows Spring conventions

### Test Organization
- **Unit tests**: Component-level testing
- **Integration tests**: Real process execution (`*IntegrationTest.java`)
- **Live agent tests**: Require API keys, tagged with `@Tag("agents-live")`
- Use `@EnabledIfEnvironmentVariable` for tests requiring external dependencies

### Maven Profiles
- **Default**: Core tests only (no external dependencies)
- **agents-live**: Live agent integration tests (requires API keys and CLI tools)

### Troubleshooting Common Issues

**Build Failures**:
1. Spring Java Format violations → Run `./mvnw spring-javaformat:apply`
2. Missing spring-ai-agents → Build sibling project first: `cd ../spring-ai-agents && ./mvnw clean install`
3. JBang path errors → Verify absolute paths in launcher configuration

**Benchmark Execution Failures**:
1. "Script not found" → Check JBang launcher path is absolute
2. Empty reports → Verify agent actually creates expected files
3. Verification failures → Check workspace contents in `bench-reports/<runId>/workspace/`

## CI/CD Pipeline

The `benchmark-ci.yml` workflow:
1. Sets up Java 17, Node.js 20, JBang
2. Installs Claude Code CLI (`@anthropic-ai/claude-code`) and Gemini CLI (`@google/gemini-cli`)
3. Runs hello-world benchmark as smoke test
4. Generates static site and deploys to GitHub Pages

## Dependencies on Spring AI Agents

This project depends on the sibling spring-ai-agents project for:
- Agent implementations (`ClaudeCodeAgentModel`, `GeminiAgentModel`)
- CLI discovery utilities (`CliDiscovery`)
- JBang launcher script at `../spring-ai-agents/jbang/launcher.java`

The integration is intentionally loose-coupled through CLI invocation to ensure independence.

## Project Evolution and Planning

### Genesis Implementation Approach

This project followed a disciplined "genesis" implementation approach with comprehensive planning:

**Planning Documents** (in `plans/` directory, gitignored):
- `genesis.md` - Complete implementation plan with acceptance checklist
- `implementation-plan.md` - Real agent integration roadmap (Claude Code, Gemini)
- `learnings.md` - Development insights and lessons learned

**Key Principle**: Build the minimal viable "heartbeat" system first to prove the integration pattern works, then expand to real agents.

### Development Methodology

**Planning-First Approach**:
1. Write comprehensive plan with acceptance criteria
2. Implement systematically following the plan
3. Update plans with learnings as you go
4. Use TodoWrite tool to track progress on multi-step tasks

**Critical Success Factors**:
- **End-to-end integration testing** - Prove JBang → spring-ai-agents works
- **Comprehensive documentation** - Future instances need complete context
- **Professional terminology** - Avoid "smoke test", use "benchmark" and "baseline"
- **Spring Java Format compliance** - Non-negotiable for consistency

### Future Development Paths

**Immediate Next Steps** (from implementation-plan.md):
1. **Real Agent Integration** - Claude Code and Gemini CLI agents
2. **Enhanced Tracks** - More benchmark cases beyond hello-world
3. **Advanced Reporting** - Cost tracking, performance metrics

**Architecture Evolution**:
- The system was designed to be modular and extensible
- Agent types are pluggable via Spring Boot auto-configuration
- The CLI → JBang → Agent pattern scales to any agent type
- Static site generation automatically handles new benchmark types