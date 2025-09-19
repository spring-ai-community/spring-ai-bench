# Integration Learnings

**Last Updated**: 2025-09-19

## What Works

### API Design Insights
- **Translation layer is valuable**: Allows both repos to evolve independently
- **Simple records are good**: Easy to work with and understand
- **Builder patterns help**: Especially for complex configurations

### Development Process
- **Start with deterministic tests**: Removes AI/network variability
- **Plans directory is useful**: Persistent tracking and learning capture
- **Small incremental steps**: Easier to debug and understand

### Build Success
- **Spring AI Agents builds successfully**: All 11 modules compiled and installed
- **Maven local repository updated**: Fresh 0.1.0-SNAPSHOT artifacts available
- **Javadoc warnings are non-blocking**: Build completed despite documentation gaps

## What to Avoid

### Implementation Pitfalls
- **Don't refactor both sides simultaneously**: Keep changes to one repo at a time
- **Don't over-engineer the adapter**: Start simple, add complexity as needed
- **Don't ignore error handling**: Translation layer needs robust error conversion

### Design Anti-patterns
- **Avoid leaky abstractions**: Keep bench and agent concerns separate
- **Don't duplicate timeouts**: Use one source of truth for timing constraints

### Environment Variable Issues
- **Don't rely on shell environment inheritance**: Maven Surefire runs in forked JVM that doesn't inherit shell env vars
- **Must explicitly configure environment in pom.xml**: Use `<environmentVariables>` in Surefire plugin configuration
- **API keys won't be passed automatically**: Need explicit Maven configuration to pass ANTHROPIC_API_KEY to test processes

## Key Decisions Made

### Architecture Choices
- **New bench-agents module**: Isolates integration code from core functionality
- **Adapter pattern**: Clean separation between different API styles
- **JSON + HTML reports**: Both machine and human readable outputs

### Technology Choices
- **Jackson for JSON**: Standard Spring choice for serialization
- **Simple HTML**: No complex JS dependencies for initial implementation
- **JUnit 5**: Consistent with rest of codebase

## Lessons for Next Steps

### When Adding Real Agents
- **Keep HelloWorld as smoke test**: Deterministic baseline for CI/CD
- **Add agent-specific adapters**: Each agent type may need custom handling
- **Enhance error reporting**: Real agents will have more complex failure modes

### For Scaling Up
- **Consider metrics collection**: Track performance across agent types
- **Plan for async execution**: Some agents may take minutes to complete
- **Think about resource limits**: Prevent runaway processes

### Gemini CLI Tool Discovery
**Key Discovery**: Gemini CLI has two operating modes:
1. **Direct prompt mode**: `gemini --yolo "prompt"` - Can create files using native capabilities
2. **Agent/interactive mode**: Uses MCP (Model Context Protocol) servers for tool registry - Only has read tools by default

Tests showed spring-ai-agents uses agent mode where tool registry only includes: `"read_file", "web_fetch", "glob"`. The `write_file` tool is missing because no MCP server provides it. However, direct CLI usage with `--yolo` successfully creates files.

### Maven Testing Configuration
- **Environment Variables in Tests**: Must configure in `pom.xml` for test execution:
  ```xml
  <plugin>
      <artifactId>maven-surefire-plugin</artifactId>
      <configuration>
          <environmentVariables>
              <ANTHROPIC_API_KEY>${env.ANTHROPIC_API_KEY}</ANTHROPIC_API_KEY>
          </environmentVariables>
      </configuration>
  </plugin>
  ```
- **Profile-specific config**: Use Maven profiles for different test environments
- **CLI subprocess inheritance**: spring-ai-agents CLITransport needs explicit environment variable passing

### Test Debugging Patterns
- **Workspace preservation for debugging**: Comment out @AfterEach cleanup when debugging failed tests
- **Test lifecycle and cleanup timing**:
  - JUnit @AfterEach runs after test completion but before manual inspection
  - Workspace cleanup should happen AFTER reports are generated and verified
  - For debugging, preserve workspaces until analysis is complete
- **Workspace inspection best practices**:
  - Always check actual workspace contents when debugging agent behavior
  - Failed agents may still create partial files that provide debugging clues
  - Don't rely only on verification results - inspect actual file system state
- **Report generation timing**: Ensure cleanup doesn't interfere with report generation and file artifact collection

---

*Add new learnings after each major milestone*