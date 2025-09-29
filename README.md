# Spring AI Bench

## Motivation

After spending hundreds of hours working with popular AI tools for software development, I've been extremely impressed by their capabilities. This is a transformative technology that continues to improve at a rapid pace.

But one question has always gnawed at me: **how do we quantify this experience?**

In presentations and product demos, you often hear confident claims about new AI stacks or workflows. What's usually missing is a **quantifiable way** to verify whether those claims hold up.

Looking deeper, most academic work has tried to answer this with benchmarks. The most prominent in software engineering are the [SWE-bench benchmarks](https://www.swebench.com/original.html). They are widely cited in research papers and marketing material from AI startups, with leaderboards maintained [here](https://www.swebench.com/).

The more I studied these benchmarks, the more I realized their approach is questionable — and I'm not alone ([Runloop blog](https://www.runloop.ai/blog/swe-bench-deep-dive-unmasking-the-limitations-of-a-popular-benchmark)). Fundamentally, SWE-bench and similar datasets operate in a way that is **not analogous to real software development**. They overwhelmingly use Python, while Java — the dominant enterprise language — is barely represented.

As someone who wants to understand how AI tools will truly help solve engineering problems, I also believe benchmarks should be **runnable on your own codebases** to evaluate practical effectiveness. Current benchmarks fall short on this dimension.

That's why I created **Spring AI Bench**:
an **open benchmarking suite for Java-centric AI developer agents**.

It fills a critical gap: today's benchmarks are Python-biased and built on outdated agent loops that misrepresent developer work. Spring AI Bench instead measures what matters for **enterprise Java development** — issue triage, PR review, integration testing, test coverage, dependency upgrades, compliance, and more.

---

## The Case for Spring AI Bench

- Benchmarks such as "classic" SWE-bench rely on **outdated agent loops** (edit → apply patch → run tests). This gave the illusion of "agency," but in reality it optimized trial-and-error patching, not developer workflows.

- Results that look decent in **Python** collapse when tested in **Java**:
  - **SWE-PolyBench (AWS, 2025):** Across agents, **Python ~20–24%** vs **Java ~11–16%** (TypeScript often just **5–13%**).
    [arXiv:2501.xxxxx](https://arxiv.org/abs/2501.xxxxx)
  - **SWE-bench-Java (2024):** Early public runs with the SWE-agent scaffold resolved only **6–10% of verified Java issues** (e.g. GPT-4o 6.6%, DeepSeek-V2 9.9% on 91 verified tasks).
    Meanwhile, the Python-only SWE-bench Verified benchmark has steadily improved, reaching **74.5% with Anthropic's Opus 4.1 (Aug 2025)** ([Anthropic blog](https://www.anthropic.com/news/opus-4-1)). In contrast, Java's early runs remain in the single digits — an **order-of-magnitude gap**.
  - **SWE-bench-Live (2025):** On new, contamination-resistant issues, even the best agent+model combos top out around **17–19%**, versus >60% on the static Verified split — strong evidence of overfitting.
    [arXiv:2505.23419](https://arxiv.org/abs/2505.23419)

**In short:** Verified Python benchmarks reach ~75%, while Verified Java benchmarks remain in the **single-digit to low-teens**. That's an **order-of-magnitude gap**.

Enterprise Java teams deserve a benchmark that reflects **real software development tasks and workflows** — and one that can be applied directly to **your own Java projects and codebases**. That's the goal of Spring AI Bench.

---

## Implementation

**Spring AI Bench** is a comprehensive **execution framework and benchmarking platform** for AI agents in Java environments. It provides isolated sandboxes, customizable execution, and evaluation capabilities for testing AI agent performance on real-world software engineering tasks.

### 🚀 Quick Start

```bash
# Clone and run a quick test
git clone https://github.com/spring-ai-community/spring-ai-bench.git
cd spring-ai-bench
./mvnw test -Dtest=BenchHarnessE2ETest
```

### 🏗️ Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Agent Types   │    │  Execution Core  │    │   Sandboxes     │
├─────────────────┤    ├──────────────────┤    ├─────────────────┤
│ ✅ Claude Code  │────│ BenchHarness     │────│LocalSandbox     │
│ ✅ Gemini       │    │ AgentRunner      │    │DockerSandbox    │
│ ✅ HelloWorld   │    │ SpecLoader       │    │CloudSandbox     │
│                 │    │ ReportGenerator  │    │   (Future)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
        │                       │                       │
        │LocalSandbox │ │DockerSandbox│ │CloudSandbox │
        │             │ │             │ │   (Future)  │
        │Process exec │ │Containers   │ │Distributed  │
        └─────────────┘ └─────────────┘ └─────────────┘
```

### 🤖 Supported Agent Types

- **`claude-code`** - Claude Code CLI integration with MCP tools ✅
- **`gemini`** - Google Gemini CLI integration with yolo mode ✅
- **`hello-world`** - Mock agent for testing infrastructure ✅

### 🎯 Benchmark Categories

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
# Clone the repository
git clone https://github.com/spring-ai-community/spring-ai-bench.git
cd spring-ai-bench

# Full build with tests (requires API keys for agent tests)
./mvnw clean install

# Quick build (skip tests)
./mvnw clean install -DskipTests

# Compile only (fastest)
./mvnw clean compile
```

### Running Tests

#### Core Tests (No API Keys Required)
```bash
# All core tests (infrastructure, sandboxes, framework)
./mvnw test

# Specific test categories
./mvnw test -Dtest=*IntegrationTest    # All integration tests
./mvnw test -Dtest=BenchHarnessE2ETest # End-to-end benchmark test
./mvnw test -Dtest=LocalSandboxIntegrationTest # Local execution tests
./mvnw test -Dtest=DockerSandboxTest   # Docker container tests
```

#### Agent Integration Tests (Requires API Keys)

**Prerequisites:**
- `ANTHROPIC_API_KEY` - For Claude Code agent testing
- `GEMINI_API_KEY` - For Gemini agent testing

**Run All Agent Tests:**
```bash
# Set your API keys and run all agent integration tests
export ANTHROPIC_API_KEY=your_claude_key
export GEMINI_API_KEY=your_gemini_key
./mvnw test -Pagents-live
```

**Run Specific Agent Tests:**
```bash
# Claude Code agent only
ANTHROPIC_API_KEY=your_key ./mvnw test -Dtest=ClaudeIntegrationTest

# Gemini agent only
GEMINI_API_KEY=your_key ./mvnw test -Dtest=GeminiIntegrationTest

# HelloWorld mock agent (no API key needed)
./mvnw test -Dtest=HelloWorldIntegrationTest
```

#### Test Profiles

- **Default profile**: Runs core infrastructure tests (no API keys required)
- **`agents-live` profile**: Runs live agent integration tests (requires API keys)
  ```bash
  ./mvnw test -Pagents-live
  ```

#### Verification Commands

```bash
# Verify everything builds and core tests pass
./mvnw clean verify

# Quick baseline test
./mvnw test -Dtest=BenchHarnessTest

# Full verification including agent tests (requires API keys)
ANTHROPIC_API_KEY=your_key GEMINI_API_KEY=your_key ./mvnw clean verify -Pagents-live
```

## Configuration

### YAML Configuration Example

```yaml
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

### Supported Features

- ✅ **Multiple Sandbox Types** - Local process, Docker containers, cloud execution
- ✅ **Agent Integration** - Claude Code and Gemini CLI support
- ✅ **Flexible Configuration** - YAML-based specification files
- ✅ **Rich Reporting** - HTML and JSON outputs with metadata
- ✅ **Verification System** - Comprehensive success criteria validation
- ✅ **MCP Tool Support** - Model Context Protocol integration
- ✅ **Comprehensive Testing** - Unit, integration, and E2E test coverage

### Testing Strategy

Spring AI Bench uses a comprehensive testing approach:

- **Unit Tests** - Individual component testing (90 tests)
- **Integration Tests** - Real process execution validation (17 tests)
- **E2E Tests** - Complete benchmark workflow testing (2 tests)
- **Baseline Tests** - Basic functionality validation (1 test)

Total: **174 tests** with 100% pass rate

## Key Features

### Recent Implementation
- ✅ **Claude Code Integration** - Full support with cost tracking and metadata
- ✅ **Gemini Integration** - Complete yolo mode configuration for file operations
- ✅ **Enhanced Reporting** - HTML reports with agent metadata and cost information
- ✅ **Verification System** - Comprehensive file and content verification
- ✅ **Maven Profiles** - Agent testing with proper environment variable configuration

### Architecture Improvements
- ✅ **Migrated to zt-exec** - Replaced ProcessBuilder with robust zt-exec library
- ✅ **Improved timeout handling** - Automatic process destruction and better error messages
- ✅ **Enhanced platform compatibility** - Windows/Unix shell command abstraction
- ✅ **Simplified test suite** - Removed complex mocks, focus on integration testing
- ✅ **Package reorganization** - Consolidated single-class packages into cohesive modules
- ✅ **Service extraction** - WorkspaceService and ReportService for better separation of concerns
- ✅ **Spring Java Format** - Proper Spring code formatting and conventions
- ✅ **AgentSpec builder pattern** - Fluent API for better test readability

---

## Tracks

Spring AI Bench defines tracks that map directly to **real enterprise developer workflows**:

- ✅ **Test Coverage Uplift**
- ✅ **Issue Analysis & Labeling**
- ✅ **Pull Request Review**
- ✅ **Integration Testing**
- ✅ **Bug Fixing**
- ✅ **Dependency Upgrades**

---

Spring AI Bench asks a bigger question:

**Can AI act as a true Java developer agent?**
- Not just fixing bugs,
- But analyzing and labeling issues,
- Reviewing pull requests,
- Running integration tests,
- Raising coverage,
- Cleaning up static analysis issues,
- Migrating APIs,
- Upgrading dependencies,
- Keeping builds compliant.

That's the standard enterprise developers hold themselves to — and the standard we should evaluate AI against.

## Contributing

We welcome contributions! Please:

- Write tests for new features
- Update documentation for API changes
- Follow Spring Java Format conventions: `./mvnw spring-javaformat:apply`
- Ensure all tests pass: `./mvnw clean test`
- Verify formatting: `./mvnw spring-javaformat:validate`

## Support

For questions, issues, or contributions, please visit our [GitHub repository](https://github.com/spring-ai-community/spring-ai-bench).

## License

Spring AI Bench is released under the Apache 2.0 license. See [LICENSE](LICENSE) for details.