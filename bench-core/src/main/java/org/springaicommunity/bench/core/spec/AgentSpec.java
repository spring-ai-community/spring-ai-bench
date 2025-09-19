package org.springaicommunity.bench.core.spec;

import java.util.Map;

public record AgentSpec(
    String kind,          // "claude-code", "goose", "openai-codex"
    String model,
    Boolean autoApprove,
    String prompt,
    Map<String,Object> genParams,
    String role         // optional; default "coder"
) {}
