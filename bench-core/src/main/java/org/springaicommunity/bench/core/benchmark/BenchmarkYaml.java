package org.springaicommunity.bench.core.benchmark;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson mapping for {@code benchmark.yaml} deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BenchmarkYaml(String schema, String name, String version, String description,
		@JsonProperty("default-timeout") String defaultTimeout, Map<String, Object> jury,
		Map<String, Object> metadata) {
}
