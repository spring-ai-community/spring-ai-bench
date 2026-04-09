# Agent Bench

📖 **[Documentation](https://springaicommunity.mintlify.app/projects/incubating/agent-bench)**

Benchmarking framework for AI coding agents on enterprise Java tasks. Defines benchmarks as YAML, launches any CLI agent, grades results with cascaded judge tiers from [Agent Judge](https://github.com/spring-ai-community/agent-judge).

## Quick Start

```bash
git clone https://github.com/spring-ai-community/agent-bench.git
cd agent-bench
./mvnw clean install -DskipTests
```

List available benchmarks:

```
$ bench list

Available benchmarks:

  code-coverage                  v1.0      (1 tasks)
  hello-world                    v1.0      (1 tasks)
```

Run a benchmark with an agent:

```bash
bench run --benchmark hello-world --agent agents/claude-code.yaml
```

## How It Works

The bench orchestrates a per-task lifecycle:

```
provide → setup scripts → agent → post scripts → grade → result.json
```

1. **Provide** copies the workspace template and writes `INSTRUCTION.md`
2. **Setup** scripts run in the workspace (clone repo, compile, measure baseline)
3. **Agent** executes — any CLI tool that reads `INSTRUCTION.md` and modifies the workspace
4. **Post** scripts run (build, test, generate reports)
5. **Grade** evaluates the workspace with a cascaded jury from [Agent Judge](https://github.com/spring-ai-community/agent-judge)

## Benchmark Format

Benchmarks live in `benchmarks/` as YAML:

```
benchmarks/code-coverage/
├── benchmark.yaml
├── prompts/
│   └── judge-practice-adherence.txt
└── tasks/
    └── spring-petclinic/
        └── task.yaml
```

### benchmark.yaml

Defines the jury — a cascaded sequence of judge tiers:

```yaml
schema: bench.benchmark.v1
name: code-coverage
version: "1.0"
description: "Write JUnit tests to maximize JaCoCo instruction coverage."
default-timeout: PT45M

jury:
  tiers:
    - name: build
      policy: REJECT_ON_ANY_FAIL
      checks:
        - type: maven-build
          goals: [clean, test]
    - name: coverage-preservation
      policy: REJECT_ON_ANY_FAIL
      checks:
        - type: coverage-preservation
    - name: coverage-improvement
      policy: ACCEPT_ON_ALL_PASS
      checks:
        - type: coverage-improvement
          min: 50.0
```

### task.yaml

Defines a single task — the problem, setup, and post-processing:

```yaml
schema: bench.task.v1
id: spring-petclinic
instruction: |
  Write JUnit tests for this Spring Boot project to maximize code coverage.
  Run ./mvnw clean test jacoco:report to measure coverage.
  Focus on behavioral code — skip Application main classes, records, and config.
  Use narrow test slices (@WebMvcTest, @DataJpaTest) over @SpringBootTest.
timeout: PT45M
metadata:
  baselineCoverage: 0.0
setup:
  - "git init && git remote add origin https://github.com/spring-projects/spring-petclinic.git && git fetch --depth 1 origin edf4db28affc && git checkout FETCH_HEAD"
  - "./mvnw clean compile -q -Dspring-javaformat.skip=true -Dcheckstyle.skip=true"
post:
  - "./mvnw clean test jacoco:report -q -Dspring-javaformat.skip=true -Dcheckstyle.skip=true"
```

### Agent Config

Agent configs are minimal — just a command and timeout:

```yaml
# agents/claude-code.yaml
command: claude --print --dangerously-skip-permissions 'Read INSTRUCTION.md and follow the instructions precisely.'
timeout: PT45M
```

The bench launches the command via `bash -c` in the workspace directory. Any CLI tool works.

## Bring Your Own Agent

The filesystem is the contract. The bench writes `INSTRUCTION.md` to the workspace; the agent reads it and modifies files. You can also run the provide/grade steps separately:

```bash
# Set up workspace
bench provide --benchmark code-coverage --task spring-petclinic --workspace /tmp/petclinic

# Run your agent (any tool that reads INSTRUCTION.md)
cd /tmp/petclinic && your-agent "$(cat INSTRUCTION.md)"

# Grade the result
bench grade --benchmark code-coverage --task spring-petclinic --workspace /tmp/petclinic
```

## CLI Reference

| Command | Purpose |
|---------|---------|
| `bench list` | List available benchmarks |
| `bench tasks --benchmark <name>` | List tasks in a benchmark |
| `bench provide --benchmark <name> --task <id> --workspace <dir>` | Set up workspace with instruction |
| `bench grade --benchmark <name> --task <id> --workspace <dir>` | Evaluate agent's work |
| `bench run --benchmark <name> --agent <config> [--task <id>]` | Full pipeline: provide + agent + grade |

## Architecture

Two modules:

- **agent-bench-core** — CLI, benchmark catalog, run orchestration, result model, judge factory
- **agent-bench-agents** — Agent-specific judge implementations (e.g., LLM-based test quality judge)

Key classes:

| Class | Role |
|-------|------|
| `BenchmarkCatalog` | Discovers benchmarks from `benchmarks/` directory |
| `BenchmarkTask` | A single task: instruction, setup/post scripts, metadata |
| `RunCommand` | Orchestrates the full lifecycle per task |
| `JudgeFactory` | Materializes YAML jury config into Judge instances |
| `TrialResult` | Per-attempt result with timestamps and scores |
| `BenchmarkResult` | Aggregate result with accuracy and pass@k |
| `ExecAgentInvoker` | Loads agent config and launches the command |

Module layering is enforced by ArchUnit — core does not depend on agents.

## Built-in Judge Types

| Type | What it checks |
|------|----------------|
| `file-exists` | File exists at path |
| `file-content` | File content matches expected (EXACT or CONTAINS) |
| `maven-build` | Maven build succeeds with specified goals |
| `coverage-preservation` | JaCoCo coverage not dropped from baseline |
| `coverage-improvement` | JaCoCo coverage exceeds threshold |

Custom judge types can be registered via `JudgeFactory.register()`.

## Programmatic Usage

```java
// Discover benchmarks
BenchmarkCatalog catalog = new BenchmarkCatalog(Path.of("benchmarks"));
List<Benchmark> benchmarks = catalog.discover();

// Find a specific benchmark
Benchmark benchmark = benchmarks.stream()
    .filter(b -> b.name().equals("code-coverage"))
    .findFirst()
    .orElseThrow();

// Access tasks
BenchmarkTask task = benchmark.tasks().get(0);
assert task.id().equals("spring-petclinic");
assert task.instruction().contains("JUnit tests");

// Wire judges from YAML config
JudgeFactory factory = new JudgeFactory();
Judge judge = factory.createFromConfig(benchmark.juryConfig());
```

## Available Benchmarks

| Benchmark | Tasks | Status |
|-----------|-------|--------|
| `hello-world` | 1 | Working — validates file creation |
| `code-coverage` | 1 (spring-petclinic) | Judges validated, end-to-end run pending |

## Related Projects

- **[Agent Judge](https://github.com/spring-ai-community/agent-judge)** — Cascaded judge framework (core dependency)
- **[Agent Client](https://github.com/spring-ai-community/agent-client)** — CLI agent integrations (Claude, Gemini)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new features
4. Ensure `./mvnw clean test` passes
5. Open a Pull Request

## License

Apache License 2.0 — see [LICENSE](LICENSE).
