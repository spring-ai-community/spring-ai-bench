# Spring AI Bench

<!-- [![Build Status](https://github.com/spring-ai-community/spring-ai-bench/workflows/CI/badge.svg)](https://github.com/spring-ai-community/spring-ai-bench/actions) -->
<!-- [![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity.bench/spring-ai-bench-parent.svg)](https://search.maven.org/search?q=g:org.springaicommunity.bench) -->

**Maven Snapshot Artifacts Coming Soon**

📖 **[Documentation](https://spring-ai-community.github.io/spring-ai-bench/)** | [Getting Started](https://spring-ai-community.github.io/spring-ai-bench/getting-started.html) | [Benchmarks](https://spring-ai-community.github.io/spring-ai-bench/benchmarks/overview.html) | [Spring AI Agents](https://github.com/spring-ai-community/spring-ai-agents)

## What & Why

Existing benchmarks (SWE-bench) measure yesterday's agents on static 2023 Python patches. They can't evaluate the agents teams actually use (Claude, Gemini, Amazon Q, Amp) on enterprise Java workflows.

**Spring AI Bench measures modern agents on real enterprise development tasks** — issue triage, PR review, coverage uplift, compliance validation, dependency upgrades. Run benchmarks on YOUR repos to measure YOUR scenarios.

**If agents have evolved, benchmarks must evolve too.**

📖 **[Read the full analysis →](https://spring-ai-community.github.io/spring-ai-bench/)** | [Why SWE-bench Falls Short](https://spring-ai-community.github.io/spring-ai-bench/#_the_evidence_why_swe_bench_falls_short)

---

## Why Different

|Dimension |SWE-bench |Spring AI Bench|
|---|---|---|
|**Scope**|Patch loops only|Full dev lifecycle (triage, PR review, coverage, compliance)|
|**Language**|Python-only (~75% scores)|Java-first (~7-10% gap shows training bias)|
|**Agent Support**|One architecture (SWE-agent)|Any agent (Claude, Gemini, Q, Amp, custom)|
|**Reproducibility**|No disclosure required|One-click Docker + open scaffolding|
|**Agent Paradigm**|2024 patch-loop agents|2025 declarative goal agents|
|**Standards**|Pre-BetterBench|Following BetterBench principles|

📖 **[See detailed evidence & analysis](https://spring-ai-community.github.io/spring-ai-bench/#_the_evidence_why_swe_bench_falls_short)** for contamination data (60%+ Verified → 19% Live), language bias analysis, and scaffolding transparency.

---

## What Spring AI Bench Does

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

**Supports any agent via `AgentModel` abstraction** — Claude Code, Gemini CLI, Amazon Q Developer, Amp, Codex, or custom implementations. Measure the agents YOUR team uses, not just academic research artifacts.

## Current Implementation (September 2024)

### What Works Today

Spring AI Bench provides a comprehensive **execution framework and benchmarking platform** for AI agents in Java environments with isolated sandboxes, customizable execution, and evaluation capabilities.

#### Available Agents
- **hello-world**: Deterministic mock agent for testing infrastructure and baseline comparisons
- **hello-world-ai**: AI-powered agent via [Spring AI Agents](https://github.com/spring-ai-community/spring-ai-agents) AgentClient integration
  - Supports Claude and Gemini providers
  - Uses JBang launcher pattern for seamless execution

#### Execution Framework
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Agent Types   │    │  Execution Core  │    │   Sandboxes     │
├─────────────────┤    ├──────────────────┤    ├─────────────────┤
│ ✅ Claude Code  │────│ BenchHarness     │────│LocalSandbox     │
│ ✅ Gemini       │    │ AgentRunner      │    │DockerSandbox    │
│ ✅ HelloWorld   │    │ SpecLoader       │    │CloudSandbox     │
│                 │    │ ReportGenerator  │    │   (Future)      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

#### Multi-Agent Benchmarking
Spring AI Bench supports comparative benchmarking between different agent implementations:

|Implementation |Duration |Performance Notes|
|---|---|---|
|**hello-world (deterministic)**|115 ms|Fast baseline for infrastructure testing|
|**hello-world-ai (Gemini provider)**|5.3 seconds|Efficient AI processing|
|**hello-world-ai (Claude provider)**|99 seconds|Thorough, detailed analysis|

*All implementations successfully completed the hello-world file creation task with 100% accuracy.*

### Benchmark Tracks: The Full Vision

Spring AI Bench is designed to support tracks that map directly to **real enterprise developer workflows** - this is what makes it different from existing benchmarks that focus only on narrow bug-fixing loops.

#### ✅ Available Now
- **hello-world**: File creation and basic infrastructure validation

#### 🚧 In Active Development
These tracks represent the core enterprise Java workflows that current benchmarks ignore:

- **Test Coverage Uplift**: Generate tests to achieve specific coverage thresholds while keeping builds green
- **Issue Analysis & Labeling**: Automated issue triage and classification using domain-specific labels
- **Pull Request Review**: Comprehensive PR analysis with structured reports, risk assessment, and policy compliance
- **Static Analysis Remediation**: Fix checkstyle violations and code quality issues while preserving functionality

#### 📋 Future Roadmap
The complete enterprise development lifecycle:

- **Integration Testing**: Validate system integration points and service boundaries
- **Bug Fixing**: Resolve real issues while maintaining build health and test coverage
- **Dependency Upgrades**: Manage Maven dependency updates with compatibility validation
- **API Migration**: Update code to use newer API versions with deprecation handling
- **Compliance Validation**: Ensure code meets enterprise security and governance standards
- **Performance Optimization**: Identify and resolve performance bottlenecks
- **Documentation Generation**: Auto-generate and maintain technical documentation

**This is the vision that sets Spring AI Bench apart** - measuring AI agents on the full spectrum of enterprise Java development, not just isolated bug fixes.

## Quick Start

### Prerequisites

For AI agent integration testing, you'll need to build and install spring-ai-agents locally:

```bash
# Build spring-ai-agents first
git clone https://github.com/spring-ai-community/spring-ai-agents.git
cd spring-ai-agents
./mvnw clean install -DskipTests
cd ..

# Then build spring-ai-bench
git clone https://github.com/spring-ai-community/spring-ai-bench.git
cd spring-ai-bench
./mvnw clean install
```

### Basic Testing

```bash
# Run hello-world benchmark via Maven exec plugin
./mvnw exec:java -pl bench-core -Dexec.args="run --run-file runs/examples/hello-world-run.yaml"

# Generate static site from benchmark results (JBang - much simpler!)
jbang jbang/site.java --reportsDir /tmp/bench-reports --siteDir /tmp/bench-site
```

### Agent Integration Testing

Set up API keys for live agent testing:

```bash
# API Keys
export ANTHROPIC_API_KEY=your_key
export GEMINI_API_KEY=your_key

# Core tests (no API keys required)
./mvnw test

# Live agent tests (requires API keys)
./mvnw test -Pagents-live

# Multi-agent comparison test (runs deterministic + AI agents)
ANTHROPIC_API_KEY=your_key GEMINI_API_KEY=your_key \
./mvnw test -Dtest=HelloWorldMultiAgentTest -pl bench-agents
```

### Viewing Reports

After running tests or benchmarks, reports are generated in multiple formats:

```bash
# Main benchmark reports index
file:///tmp/bench-reports/index.html

# Generated benchmark site (after running site generator)
file:///tmp/bench-site/index.html
```

## Architecture

Spring AI Bench is built around a **Sandbox abstraction** that provides isolated execution environments:

- **LocalSandbox**: Direct process execution (fast, development)
- **DockerSandbox**: Container isolation (secure, production-ready)
- **CloudSandbox**: Distributed execution (planned)

Key components:
- `BenchHarness`: End-to-end benchmark execution
- `AgentRunner`: Agent execution interface with Spring AI Agents integration
- `SuccessVerifier`: Validation of benchmark results *(temporary implementation - will evolve into judge concept in spring-ai-agents)*
- `ReportGenerator`: HTML and JSON report generation

For detailed architecture information, see [Architecture Documentation](https://spring-ai-community.github.io/spring-ai-bench/architecture.html).

## Integration with Spring AI Agents

Spring AI Bench integrates with the [Spring AI Agents](https://github.com/spring-ai-community/spring-ai-agents) framework via JBang execution:

```bash
# Pattern: spring-ai-bench → JBang → spring-ai-agents → AI provider
jbang /path/to/spring-ai-agents/jbang/launcher.java \
  hello-world-agent-ai \
  path=hello.txt \
  content="Hello World!" \
  provider=claude
```

This ensures benchmark success guarantees good end-user experience by testing the exact CLI interface users would use.

## Run It Yourself: The Big Differentiator

Unlike static benchmarks, Spring AI Bench runs on YOUR repos to measure YOUR scenarios:

```bash
# 1. Clone and build dependencies (5 minutes)
git clone https://github.com/spring-ai-community/spring-ai-agents.git
cd spring-ai-agents && ./mvnw clean install -DskipTests

git clone https://github.com/spring-ai-community/spring-ai-bench.git
cd spring-ai-bench

# 2. Set your API keys
export ANTHROPIC_API_KEY=your_key
export GEMINI_API_KEY=your_key

# 3. Run on YOUR codebase
./mvnw test -Dtest=HelloWorldMultiAgentTest -pl bench-agents

# 4. View results in your browser
open file:///tmp/bench-reports/index.html
```

**Curated golden benchmark set** (for standardized comparison) **+ run same benchmarks on YOUR repos** (to measure real-world effectiveness). Get results you can trust.

## Documentation

For comprehensive documentation, visit:

- 📖 **[Full Documentation](https://spring-ai-community.github.io/spring-ai-bench/)**
- 📖 **[Getting Started Guide](https://spring-ai-community.github.io/spring-ai-bench/getting-started.html)**
- 📖 **[Architecture Overview](https://spring-ai-community.github.io/spring-ai-bench/architecture.html)**
- 📖 **[Running Benchmarks](https://spring-ai-community.github.io/spring-ai-bench/benchmarks/running-benchmarks.html)**
- 📖 **[Agent Configuration](https://spring-ai-community.github.io/spring-ai-bench/agents/claude-code.html)**

## Related Projects

- **[Spring AI Agents](https://github.com/spring-ai-community/spring-ai-agents)**: Autonomous CLI agent integrations for the Spring AI ecosystem
- **[Spring AI](https://github.com/spring-projects/spring-ai)**: The Spring AI framework

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Spring Java formatting conventions: `./mvnw spring-javaformat:apply`
- Write tests for new features
- Update documentation for API changes
- Ensure all tests pass: `./mvnw clean test`

## Support

- **Issues**: [GitHub Issues](https://github.com/spring-ai-community/spring-ai-bench/issues)
- **Documentation**: [Documentation Site](https://spring-ai-community.github.io/spring-ai-bench/)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---

**Spring AI Bench** - *Open benchmarking suite for Java-centric AI developer agents*