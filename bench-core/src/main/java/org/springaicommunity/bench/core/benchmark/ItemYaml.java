package org.springaicommunity.bench.core.benchmark;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Jackson mapping for {@code item.yaml} deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemYaml(String schema, String id, String instruction, String timeout, Map<String, Object> metadata) {
}
