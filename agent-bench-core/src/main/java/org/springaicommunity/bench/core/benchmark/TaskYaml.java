package org.springaicommunity.bench.core.benchmark;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Jackson mapping for {@code task.yaml} deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskYaml(String schema, String id, String instruction, String timeout, String difficulty,
		Map<String, Object> metadata, List<String> setup, List<String> post) {
}
